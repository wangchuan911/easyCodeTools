package my.hehe.demo;

import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import my.hehe.demo.common.HtmlTemplateUtils;
import my.hehe.demo.common.annotation.Verticle;
import my.hehe.demo.services.FilesCatcher;
import my.hehe.demo.services.FilesDeploy;
import my.hehe.demo.services.WebServiceClient;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Verticle(worker = false)
public class WebVerticle extends AbstractVerticle {
	Pattern p = Pattern.compile("(-){10,}");
	JsonObject serverConfig = null;
	boolean isClent = true;
	boolean isServer = true;
	TemplateEngine engine;

	@Override
	public void start(Promise<Void> startFuture) throws Exception {
		Handler bodyHandler = BodyHandler.create();
		serverConfig = config().getJsonObject("server");
		isClent = serverConfig.getString("mode", "client").equals("client");
		isServer = serverConfig.getString("mode", "server").equals("server");

		FilesCatcher filesCatcher = FilesCatcher.createProxy(vertx);
		FilesDeploy filesDeploy = FilesDeploy.createProxy(vertx);
		Router router = Router.router(vertx);
		this.engine = HtmlTemplateUtils.createTemplateEngine(this.vertx);
		router.get("/*").handler(TemplateHandler.create(engine));
		router.post("/catchFile").handler(bodyHandler).blockingHandler(routingContext -> {
			HttpServerRequest httpServerRequest = routingContext.request();
			Set<String> listSet = new HashSet<>();
			{
				MultiMap params = httpServerRequest.formAttributes();
				String string = params.get("text");
				if (StringUtil.isNullOrEmpty(string) || !isClent) {
					/*htmlEngine.render(new JsonObject().put("msg", "fail!"), "templates/result.html", res -> {
						if (res.succeeded()) {
							routingContext.response().putHeader("Content-Type", "text/html").end(res.result());
						} else {
							res.cause().printStackTrace();
							routingContext.fail(res.cause());
						}
					});*/
					this.goResultHtml(new JsonObject().put("msg", "fail!"), routingContext);
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
					/*htmlEngine.render(, "templates/result.html", res -> {
						if (res.succeeded()) {
							routingContext.response().putHeader("Content-Type", "text/html").end(res.result());
						} else {
							res.cause().printStackTrace();
							routingContext.fail(res.cause());
						}
					});*/
					HtmlTemplateUtils.goHtml(engine, new JsonObject().put("msg", stringAsyncResult.cause().getMessage()), routingContext, "result");
				}
			});
		});

		router.post("/deployFile").handler(bodyHandler).blockingHandler(routingContext -> {
			if (!isServer) {
				routingContext.fail(new Throwable("失败"));
				return;
			}
			Set<FileUpload> fileUploads = routingContext.fileUploads();
      /*final AtomicInteger atomicInteger = new AtomicInteger(fileUploads.size());
      for (FileUpload fileUpload : fileUploads) {
        String uploadFile = fileUpload.uploadedFileName();
        filesDeploy.dual(uploadFile, stringAsyncResult -> {
          atomicInteger.decrementAndGet();
          StringBuilder result = new StringBuilder();
          if (stringAsyncResult.failed()) {
            result.append(stringAsyncResult.cause().getMessage()).append('\n');
          } else {
            result.append(stringAsyncResult.result()).append('\n');
          }
          if (atomicInteger.get() == 0) {
            File f = new File(uploadFile);
            if (f.exists()) {
              System.out.println(f.getName());
              f.delete();
            }
            this.goResultHtml(engine, new JsonObject().put("msg", result.toString()), routingContext);
          }
        });
      }*/
			StringBuilder result = new StringBuilder();
			CompositeFuture.all(fileUploads.stream().map(fileUpload ->
					Future.future(promise -> {
						String uploadFile = fileUpload.uploadedFileName();
						filesDeploy.dual(uploadFile, stringAsyncResult -> {
							if (stringAsyncResult.failed()) {
								result.append(stringAsyncResult.cause().getMessage()).append('\n');
								promise.fail(stringAsyncResult.cause());
							} else {
								result.append(stringAsyncResult.result()).append('\n');
								promise.complete();
							}
						});
					})
			).collect(Collectors.toList())).onSuccess(compositeFuture -> {
				fileUploads.forEach(fileUpload -> {
					File f = new File(fileUpload.uploadedFileName());
					if (f.exists()) {
						System.out.println(f.getName());
						f.delete();
					}
				});
				this.goResultHtml(new JsonObject().put("msg", result.toString()), routingContext);
			});
		}).failureHandler(routingContext -> {
			this.goResultHtml(new JsonObject().put("msg", routingContext.failure().getMessage()), routingContext);
		});

		router.post("/wsdl").handler(bodyHandler).handler(routingContext -> {
			MultiMap map = routingContext.request().formAttributes();
			String string = map.get("params");
			try {
				string = URLDecoder.decode(string, "utf-8");
			} catch (Throwable e) {
				e.printStackTrace();
			}
			String[] list = string.split("\r\n");
			WebServiceClient.KeyValue[] kvs = Arrays.stream(list).map(s -> {
				int i = s.indexOf(":");
				return WebServiceClient.keyValue(s.substring(0, i), s.substring(i + 1));
			}).toArray(WebServiceClient.KeyValue[]::new);
			String wsdl = map.get("wsdl"), qName = map.get("qName"), method = map.get("method");
			try {
				if (StringUtils.isNotEmpty(map.get("pj"))) {
					JsonObject pj = config().getJsonObject("wsdl").getJsonObject(map.get("pj"));
					wsdl = pj.getString("wsdl");
					qName = pj.getString("qName");
				}
			} catch (NullPointerException e) {

			}
			try {
				routingContext.response().end(WebServiceClient.callRpc(wsdl, qName, method, kvs).toString());
			} catch (Throwable e) {
				routingContext.fail(e);
			}
		}).failureHandler(routingContext -> {
			this.goResultHtml(new JsonObject().put("msg", routingContext.failure().getMessage()), routingContext);
		});
		int port = serverConfig.getJsonObject("port").getInteger("web");

		vertx.createHttpServer(new HttpServerOptions().setMaxFormAttributeSize(1024 * 1024)).requestHandler(router).listen(port, http -> {
			if (http.succeeded()) {
				startFuture.complete();
				System.out.println("HTTP server started http://localhost:" + port + "/get.html");
			} else {
				startFuture.fail(http.cause());
			}
		});
	}

	private void goResultHtml(JsonObject jsonObject, RoutingContext routingContext) {
		HtmlTemplateUtils.goHtml(this.engine, jsonObject, routingContext, "result");
	}


}
