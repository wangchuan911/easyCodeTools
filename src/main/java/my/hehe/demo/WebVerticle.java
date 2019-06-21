package my.hehe.demo;

import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import my.hehe.demo.common.JdbcUtils;
import my.hehe.demo.services.FilesCatcher;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebVerticle extends AbstractVerticle {


  @Override
  public void start(Future<Void> startFuture) throws Exception {
    initUtils();
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

  private void initUtils() {
    try {
      JDBCClient rimdbTest = null;
      {
        JsonObject dbConfig = new JsonObject();
        String var1 = config().getJsonObject("data").getString("data3");
        String[] var2 = var1.split("\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*");
        dbConfig.put("url", var2[0]);
        dbConfig.put("user", var2[1]);
        dbConfig.put("password", var2[2]);
        rimdbTest = JDBCClient.createShared(vertx, dbConfig);
      }

      Map<String, JDBCClient> jdbcClients = null;
      {
        Field field = JdbcUtils.class.getDeclaredField("jdbcClients");
        field.setAccessible(true);
        Object value = field.get(null);
        if (value == null) {
          field.set(null, jdbcClients = new HashMap<>());
        } else {
          jdbcClients = (Map<String, JDBCClient>) value;
        }
      }
      jdbcClients.put("rimdbTest", rimdbTest);
      JdbcUtils.getJdbcClient("rimdbTest").queryWithParams("select * from rme_eqp a where a.eqp_id=?", new JsonArray().add("000125110000000006328795"), resultSetAsyncResult -> {
        if (resultSetAsyncResult.succeeded()) {
          System.out.println(resultSetAsyncResult.result().getRows());
        } else {
          resultSetAsyncResult.cause().printStackTrace();
        }
      });
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
