package my.hehe.demo;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.annotation.ReflectionUtils;
import my.hehe.demo.common.annotation.UtilsInital;
import my.hehe.demo.common.annotation.Verticle;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Consumer;

public class ApplicationVerticle extends AbstractVerticle {
	@Override
	public void start() throws Exception {
		this.toolInit();
		deploy().accept(vertx);
	}

	Consumer<Vertx> deploy() {
		Consumer<Vertx> runner = vertx -> {
			Reflections reflections = ReflectionUtils.getReflection();
			Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Verticle.class);
			for (Class<?> verticleClass : classes) {
				DeploymentOptions options = new DeploymentOptions().setConfig(vertx.getOrCreateContext().config());
				Verticle annotation = (Verticle) verticleClass.getAnnotation(Verticle.class);
				if ((annotation).worker()) {
					options.setWorker(annotation.worker());
					options.setInstances(annotation.instances());
				}
				vertx.deployVerticle(verticleClass.getName(), options, stringAsyncResult -> {
					if (stringAsyncResult.succeeded()) {
						System.out.println(String.format("%s deploy success", stringAsyncResult.result()));
					} else {
						stringAsyncResult.cause().printStackTrace();
					}
				});
			}
		};
		return runner;
	}

	void toolInit() {
		Reflections reflections = ReflectionUtils.getReflection();
		Set<Constructor> constructors = reflections.getConstructorsAnnotatedWith(UtilsInital.class);
		for (Constructor constructor : constructors) {
			constructor.setAccessible(true);
			try {
				Class<?>[] types = constructor.getParameterTypes();
				Object[] args = new Object[types.length];
				for (int i = 0; i < types.length; i++) {
					Class<?> type = types[i];
					if (type == Vertx.class) {
						args[i] = vertx;
					} else if (type == JsonObject.class) {
						args[i] = config();
					} else {
						args[i] = null;
					}
				}
				constructor.newInstance(args);

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
