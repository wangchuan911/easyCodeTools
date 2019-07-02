package my.hehe.demo;

import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
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

public class WebVerticle extends AbstractVerticle {
  Pattern p = Pattern.compile("(-){10,}");

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Handler bodyHandler = BodyHandler.create();
    TemplateEngine engine = ThymeleafTemplateEngine.create(vertx);
    {
      // 定时模板解析器,表示从类加载路径下找模板
      ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
      /*// 设置模板的前缀，我们设置的是templates目录
      templateResolver.setPrefix("templates");
      // 设置后缀为.html文件
      templateResolver.setSuffix(".html");*/
      templateResolver.setTemplateMode("HTML5");
      templateResolver.setCharacterEncoding("utf-8");
      ((ThymeleafTemplateEngine) engine).getThymeleafTemplateEngine().setTemplateResolver(templateResolver);
    }
    TemplateHandler handler = TemplateHandler.create(engine);
    FilesCatcher filesCatcher = FilesCatcher.createProxy(vertx);
    FilesDeploy filesDeploy = FilesDeploy.createProxy(vertx);
    Router router = Router.router(vertx);
    router.get("/*").handler(handler);
    router.post("/catchFile").handler(bodyHandler).blockingHandler(routingContext -> {
      HttpServerRequest httpServerRequest = routingContext.request();
      Set<String> listSet = new HashSet<>();
      {
        MultiMap params = httpServerRequest.formAttributes();
        String string = params.get("text");
        if (StringUtil.isNullOrEmpty(string)) {
          engine.render(new JsonObject().put("msg", "fail!"), "templates/result.html", res -> {
            if (res.succeeded()) {
              routingContext.response().putHeader("Content-Type", "text/html").end(res.result());
            } else {
              res.cause().printStackTrace();
              routingContext.fail(res.cause());
            }
          });
          return;
        }
        try {
          string = URLDecoder.decode(string, "utf-8");
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        boolean textMode = false;
        StringBuilder mode = new StringBuilder();
        String[] list = string.split("\r\n");
        for (String str : list) {
          if (!textMode) {
            {
              String[] text = p.split(str);
              if (text.length == 3 && "<".equals(text[0]) && !"end".equals(text[1].toLowerCase()) && ">".equals(text[2])) {
                textMode = true;
                mode.setLength(0);
                if (text[1].indexOf(":") < 0) {
                  mode.append("TEXT:");
                }
                mode.append(text[1]).append(':');
                continue;
              }
            }
            if (StringUtil.isNullOrEmpty(str)) continue;
            listSet.add(str.trim());
          } else {
            String[] text = p.split(str);
            if (text.length == 3 && "<".equals(text[0]) && "end".equals(text[1].toLowerCase()) && ">".equals(text[2])) {
              textMode = false;
              listSet.add(mode.toString());
              mode.setLength(0);
            } else {
              mode.append(str).append("\r\n");
            }
          }
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
//          routingContext.response().end("success!");
          engine.render(new JsonObject().put("msg", stringAsyncResult.cause().getMessage()), "templates/result.html", res -> {
            if (res.succeeded()) {
              routingContext.response().putHeader("Content-Type", "text/html").end(res.result());
            } else {
              res.cause().printStackTrace();
              routingContext.fail(res.cause());
            }
          });
        }
      });
    });
    router.post("/deployFile").handler(bodyHandler).blockingHandler(routingContext -> {
      Set<FileUpload> fileUploads = routingContext.fileUploads();
      final AtomicInteger atomicInteger = new AtomicInteger(fileUploads.size());
      for (FileUpload fileUpload : fileUploads) {
        String uploadFile = fileUpload.uploadedFileName();
        filesDeploy.dual(uploadFile, stringAsyncResult -> {
          atomicInteger.decrementAndGet();
          if (stringAsyncResult.failed()) return;
          if (atomicInteger.get() == 0) {
            File f = new File(uploadFile);
            if (f.exists()) {
              System.out.println(f.getName());
              f.delete();
            }
            this.goResultHtml(engine, new JsonObject().put("msg", "上传完成"), routingContext);
            return;
          }
        });
      }
    });
    int port = config().getJsonObject("server").getJsonObject("port").getInteger("web");
    vertx.createHttpServer().requestHandler(router).listen(port, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        System.out.println("HTTP server started http://localhost:" + port + "/index.html");
      } else {
        startFuture.fail(http.cause());
      }
    });
  }

  private void goResultHtml(TemplateEngine engine, JsonObject jsonObject, RoutingContext routingContext) {
    engine.render(new JsonObject().put("msg", "上传完成"), "templates/result.html", res -> {
      if (res.succeeded()) {
        routingContext.response().putHeader("Content-Type", "text/html").end(res.result());
      } else {
        res.cause().printStackTrace();
        routingContext.fail(res.cause());
      }
    });
  }

}
