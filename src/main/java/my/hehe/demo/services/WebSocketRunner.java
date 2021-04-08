package my.hehe.demo.services;

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.annotation.ReflectionUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class WebSocketRunner {


	public abstract boolean open(ServerWebSocket serverWebSocket);

	public abstract void close(ServerWebSocket serverWebSocket);

	public static Set<WebSocketRunner> init(Vertx vertx) {
		JsonObject webSocketConfig = vertx.getOrCreateContext().config().getJsonObject("webSocket");
		Reflections reflections = ReflectionUtils.getReflection();
		Set<Class<? extends WebSocketRunner>> classes = reflections.getSubTypesOf(WebSocketRunner.class);
		Set<WebSocketRunner> socketRunnerHashSet = classes.stream().map(aClass -> {
			try {
				return aClass.getConstructor(JsonObject.class).newInstance(webSocketConfig);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toSet());
		return socketRunnerHashSet;
	}

}


