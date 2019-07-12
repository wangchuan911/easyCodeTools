package my.hehe.demo;

import com.sun.security.ntlm.Server;
import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import my.hehe.demo.common.annotation.Verticle;
import my.hehe.demo.services.FilesCatcher;
import my.hehe.demo.services.FilesDeploy;
import my.hehe.demo.services.TailRunner;
import my.hehe.demo.services.WebSocketRunner;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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
    isClent = serverConfig.getString("mode", "client").equals("client");
    isServer = serverConfig.getString("mode", "server").equals("server");

    TemplateHandler handler = TemplateHandler.create(engine);
    Router router = Router.router(vertx);

    Set<WebSocketRunner> webSocketRunners = this.initWebSocketRunners();

    int port = serverConfig.getJsonObject("port").getInteger("web") + 1;
    router.get("/*").handler(routingContext -> {
//      routingContext.response().setChunked(true);
//      routingContext.next();
      WebVerticle.goHtml(engine, new JsonObject().put("ip", "localhost").put("port", port).put("pj", routingContext.request().getParam("pj")), routingContext, "tail");
    });

    vertx.createHttpServer().websocketHandler(serverWebSocket -> {
      boolean isConnect = false;
      for (WebSocketRunner webSocketRunner :
        webSocketRunners) {
        if (isConnect = webSocketRunner.urlCheck(serverWebSocket.path())) {
          serverWebSocket.frameHandler(webSocketFrame -> {
            String id = webSocketFrame.textData();
            webSocketRunner.start(id, new JsonObject(), serverWebSocket);
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
        System.out.println("websocket server started http://localhost:" + port + "/tail.html?pj=111");
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
