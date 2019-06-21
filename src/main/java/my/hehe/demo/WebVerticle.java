package my.hehe.demo;

import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import my.hehe.demo.services.FilesCatcher;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

public class WebVerticle extends AbstractVerticle {


  @Override
  public void start(Future<Void> startFuture) throws Exception {

    Handler bodyHandler = BodyHandler.create();
    TemplateEngine engine = ThymeleafTemplateEngine.create(vertx);
    TemplateHandler handler = TemplateHandler.create(engine);
    FilesCatcher filesCatcher = FilesCatcher.createProxy(vertx);
    Router router = Router.router(vertx);
    router.get("/*").handler(handler);
    router.post("/catchFile").handler(bodyHandler).blockingHandler(routingContext -> {
      HttpServerRequest httpServerRequest = routingContext.request();
      Set<String> listSet = new HashSet<>();
      {
        MultiMap params = httpServerRequest.formAttributes();
        String string = params.get("text");
        if (StringUtil.isNullOrEmpty(string)) {
          routingContext.response().end("fail!");
          return;
        }
        try {
          string = URLDecoder.decode(string, "utf-8");
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        String[] list = string.split("\r\n");
        for (String str : list) {
          if (StringUtil.isNullOrEmpty(str)) continue;
          listSet.add(str.trim());
        }
      }

      filesCatcher.dual(listSet, stringAsyncResult -> {
        if (stringAsyncResult.succeeded()) {
          System.out.println("success!");
          HttpServerResponse httpServerResponse = routingContext.response();
          httpServerResponse.sendFile(stringAsyncResult.result(), resultHandler -> {
            if (resultHandler.succeeded()) {
              System.out.println("success!!!!!!!!");
            }
            try {
              new File(stringAsyncResult.result()).delete();
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
        } else {
          stringAsyncResult.cause().printStackTrace();
          routingContext.response().end("success!");
        }
      });
    });
    vertx.createHttpServer().requestHandler(router).listen(8088, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        System.out.println("HTTP server started http://localhost:8088/index.html");
      } else {
        startFuture.fail(http.cause());
      }
    });
  }

}
