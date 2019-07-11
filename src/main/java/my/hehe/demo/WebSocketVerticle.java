package my.hehe.demo;

import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import my.hehe.demo.common.annotation.Verticle;
import my.hehe.demo.services.FilesCatcher;
import my.hehe.demo.services.FilesDeploy;
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

    Router router = Router.router(vertx);
    int port = serverConfig.getJsonObject("port").getInteger("web") + 1;
    router.get("/tail.html").handler(routingContext -> {
      WebVerticle.goHtml(engine, new JsonObject().put("ip", "localhost").put("port", port), routingContext, "tail");
    });
    vertx.createHttpServer().websocketHandler(serverWebSocket -> {
      final AtomicInteger i = new AtomicInteger(0);
      serverWebSocket.frameHandler(webSocketFrame -> {
        serverWebSocket.writeTextMessage(i.incrementAndGet() + "\n");
      });
      serverWebSocket.closeHandler(aVoid -> {
        System.out.println("close!");
      });
    }).requestHandler(router).listen(port, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        System.out.println("websocket server started http://localhost:" + port + "/tail.html");
      } else {
        startFuture.fail(http.cause());
      }
    });
  }
}
