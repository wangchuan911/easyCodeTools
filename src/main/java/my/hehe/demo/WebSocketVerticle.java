package my.hehe.demo;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.TemplateHandler;
import my.hehe.demo.common.annotation.Verticle;

import my.hehe.demo.services.TailRunner;
import my.hehe.demo.services.WebSocketRunner;

import java.util.HashSet;
import java.util.Set;

@Verticle(worker = false)
public class WebSocketVerticle extends AbstractVerticle {
  JsonObject serverConfig = null;
  boolean isClent = true;
  boolean isServer = true;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
//    Handler bodyHandler = BodyHandler.create();
    TemplateEngine engine = WebVerticle.createTemplateEngine(vertx);
    serverConfig = config().getJsonObject("server");
    JsonObject webSocketConfig = config().getJsonObject("webSocket");

    isServer = serverConfig.getString("mode", "server").equals("server");

    if (!isServer) {
      startFuture.complete();
      return;
    }
    TemplateHandler handler = TemplateHandler.create(engine);
    Router router = Router.router(vertx);

    Set<WebSocketRunner> webSocketRunners = this.initWebSocketRunners();

    int port = serverConfig.getJsonObject("port").getInteger("webSocket");
    router.get("/*")
      .handler(handler);

    vertx.createHttpServer().websocketHandler(serverWebSocket -> {
      boolean isConnect = false;
      for (WebSocketRunner webSocketRunner :
        webSocketRunners) {
        if (isConnect = webSocketRunner.urlCheck(serverWebSocket.path())) {
          JsonObject config = webSocketConfig.getJsonObject(serverWebSocket.path().substring(1));
          serverWebSocket.frameHandler(webSocketFrame -> {
            String id = webSocketFrame.textData();
            if (config.containsKey(id)) {
              webSocketRunner.start(id, config.getJsonObject(id), serverWebSocket);
            }
          });
          serverWebSocket.closeHandler(aVoid -> {
            System.out.println("close!");
            webSocketRunner.done(serverWebSocket);
          });
        }
      }
      if (!isConnect) serverWebSocket.reject();
    }).requestHandler(router).listen(port, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        System.out.println("websocket server started http://localhost:" + port + "/tail.html?pj=ires");
      } else {
        startFuture.fail(http.cause());
      }
    });
  }

  private Set<WebSocketRunner> initWebSocketRunners() {
    Set<WebSocketRunner> socketRunnerHashSet = new HashSet<>();
    socketRunnerHashSet.add(new TailRunner());
    return socketRunnerHashSet;
  }
}
