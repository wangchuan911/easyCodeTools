package my.hehe.demo.services;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

public abstract class WebSocketRunner {
  public boolean urlCheck(String url) {
    return false;
  }

  public abstract void start(String id, JsonObject config, ServerWebSocket serverWebSocket);

  public abstract void done(ServerWebSocket serverWebSocket);

  static class STATE {
    final static int RUNNING;
    final static int START;
    final static int FAIL;
    final static int FINISH;

    static {
      int code = 0;
      RUNNING = code++;
      START = code++;
      FAIL = code++;
      FINISH = code++;
    }

  }
}


