package my.hehe.demo.services;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.StreamUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class TailRunner extends WebSocketRunner {
  private Set<TailExcutor> tailExcutors = new LinkedHashSet<>();

  public boolean urlCheck(String url) {
    return "/tail".equals(url);
  }

  public void start(String id, JsonObject config, ServerWebSocket serverWebSocket) {
    TailExcutor tailExcutor = null;
    for (TailExcutor excutor : tailExcutors) {
      if (excutor.STATE == STATE.FINISH || excutor.STATE == STATE.FAIL) {
        tailExcutors.remove(excutor);
        continue;
      }
      if (id.equals(excutor.id)) {
        tailExcutor = excutor;
        if (tailExcutor.isAlive()) {
          System.out.println(String.format("add socket to thread: %s", tailExcutor.toString()));
          tailExcutor.webSocketSet.add(serverWebSocket);
          return;
        } else {
          tailExcutors.remove(tailExcutor);
          tailExcutor = null;
        }
        continue;
      }
    }
    if (tailExcutor == null) {
      tailExcutor = new TailExcutor(id);
      tailExcutor.path = (config.getString("path"));
//      if (!new File(tailExcutor.path).exists()) return;
    }
    try {
      tailExcutor.webSocketSet.add(serverWebSocket);
      tailExcutor.start();
      tailExcutors.add(tailExcutor);
      System.out.println(String.format("create thread: %s", tailExcutor.toString()));
    } catch (Throwable e) {
      e.printStackTrace();
      tailExcutors.remove(tailExcutor);
    }
  }

  public void done(ServerWebSocket serverWebSocket) {
    tailExcutors.forEach(tailExcutor -> {
      synchronized (tailExcutor.webSocketSet) {
        tailExcutor.webSocketSet.remove(serverWebSocket);
      }
    });
  }
}

class TailExcutor extends Thread {
  final Set<ServerWebSocket> webSocketSet;
  final String id;
  String path;
  BufferedReader bufferedReader;
  Process process = null;
  int STATE = WebSocketRunner.STATE.START;

  public TailExcutor(String id) {
    this.id = id.toString();
    this.webSocketSet = new ConcurrentHashSet<>();
  }


  @Override
  public void run() {
    if (STATE == WebSocketRunner.STATE.FAIL || STATE == WebSocketRunner.STATE.FINISH || STATE == WebSocketRunner.STATE.RUNNING)
      return;
    this.init();
    String line = null;
    try {
      while ((line = bufferedReader.readLine()) != null) {
//        Thread.sleep(2000);
        if (webSocketSet.size() > 0) {
          for (ServerWebSocket socket : webSocketSet) {
            try {
              socket.writeTextMessage(line + "\n");
            } catch (Throwable e) {
              synchronized (webSocketSet) {
                webSocketSet.remove(socket);
              }
            }
          }
        } else {
          done();
          break;
        }
      }
    } catch (Throwable e) {
      this.fail(e);
    } finally {
      if (bufferedReader != null) {
        StreamUtils.close(bufferedReader);
      }
    }
  }

  private void init() {
    this.STATE = WebSocketRunner.STATE.RUNNING;
    try {
      String command = " tail -f " + path;
//      command = "ping -t localhost";
      System.out.println(String.format("run command [%s]", command));
      process = Runtime.getRuntime().exec(command);
      bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    } catch (Throwable e) {
      this.fail(e);
    }
  }


  public void done() {
    this.STATE = WebSocketRunner.STATE.FINISH;
    if (process != null) {
      try {
        process.destroy();
      } catch (Throwable e) {
      }
    }
    webSocketSet.clear();
    System.out.println(String.format("%s is ide ,release!", this.toString()));
  }

  private void fail(Throwable e) {
    if (process != null) {
      try {
        process.destroy();
      } catch (Throwable e1) {
      }
    }
    webSocketSet.clear();
    this.STATE = WebSocketRunner.STATE.FAIL;
    e.printStackTrace();
  }

}
