package my.hehe.demo.services;

import com.sun.istack.internal.NotNull;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
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
      if (excutor.done || excutor.fail != null) {
        tailExcutors.remove(excutor);
        continue;
      }
      if (id.equals(excutor.id)) {
        tailExcutor = excutor;
        if (tailExcutor.isAlive()) {
          System.out.println(String.format("create thread: %s", tailExcutor.toString()));
          tailExcutor.webSocketSet.add(serverWebSocket);
          return;
        }
        continue;
      }
    }
    if (tailExcutor == null) {
      tailExcutor = new TailExcutor(id);
      tailExcutor.setPath(config.getString("path"));
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
  Throwable fail = null;
  BufferedReader bufferedReader;
  Process process = null;
  boolean done = false;

  public TailExcutor(@NotNull String id) {
    this.id = id.toString();
    this.webSocketSet = new ConcurrentHashSet<>();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public void run() {
    this.init();
    if (fail != null) return;
    String line = null;
    try {
      while ((line =/* bufferedReader.readLine()*/"hehe") != null) {
        Thread.sleep(2000);
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
    }
  }

  private void init() {
    /*try {
      process = Runtime.getRuntime().exec(" tail -f " + path);
      bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      process.destroy();
    } catch (Throwable e) {
      this.fail(e);
    }*/
  }

  public void done() {
    done = true;
    if (process != null) {
      try {
        process.destroy();
      } catch (Throwable e) {
      }
    }
    webSocketSet.clear();
  }

  private void fail(Throwable e) {
    fail = e;
    e.printStackTrace();
  }

  public boolean isDone() {
    return done;
  }
}
