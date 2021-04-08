package my.hehe.demo;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.TemplateHandler;
import my.hehe.demo.common.HtmlTemplateUtils;
import my.hehe.demo.common.annotation.Verticle;

import my.hehe.demo.services.TailRunner;
import my.hehe.demo.services.WebSocketRunner;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Verticle(worker = false)
public class WebSocketVerticle extends AbstractVerticle {
	JsonObject serverConfig = null;
	boolean isClent = true;
	boolean isServer = true;

	@Override
	public void start(Promise<Void> startFuture) throws Exception {
//    Handler bodyHandler = BodyHandler.create();
		TemplateEngine engine = HtmlTemplateUtils.createTemplateEngine(vertx);
		serverConfig = config().getJsonObject("server");

		isServer = serverConfig.getString("mode", "server").equals("server");

		if (!isServer) {
			startFuture.complete();
			return;
		}
//		TemplateHandler handler = TemplateHandler.create(engine);
		Router router = Router.router(vertx);

		Set<WebSocketRunner> webSocketRunners = WebSocketRunner.init(vertx);

		int port = serverConfig.getJsonObject("port").getInteger("webSocket");
		router.get("/*")
				.handler(TemplateHandler.create(engine));

		vertx.createHttpServer().webSocketHandler(serverWebSocket -> {
			Optional<WebSocketRunner> isConnect = webSocketRunners
					.stream()
					.filter(webSocketRunner -> webSocketRunner.open(serverWebSocket))
					.findFirst();
			if (!isConnect.isPresent()) {
				serverWebSocket.reject();
			}
		}).requestHandler(router).listen(port, http -> {
			if (http.succeeded()) {
				startFuture.complete();
				System.out.println("websocket server started http://localhost:" + port + "/tail.html?pj=ires");
			} else {
				startFuture.fail(http.cause());
			}
		});
	}


}
