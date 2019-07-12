package my.hehe.demo.services;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

public abstract class WebSocketRunner {
  public boolean urlCheck(String url) {
    return false;
  }

  public abstract void start(String id, JsonObject config, ServerWebSocket serverWebSocket);

  public abstract void done(ServerWebSocket serverWebSocket);

}
