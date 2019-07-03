package my.hehe.demo;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.annotation.ReflectionUtils;
import my.hehe.demo.common.annotation.UtilsInital;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Consumer;

public class ApplicationVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    this.toolInit();
    deploy().accept(vertx);
  }

  private static Consumer<Vertx> deploy() {
    Consumer<Vertx> runner = vertx -> {
      DeploymentOptions workerDeploymentOptions = new DeploymentOptions()
        .setWorker(true)
        // As worker verticles are never executed concurrently by Vert.x by more than one thread,
        // deploySingle multiple instances to avoid serializing requests.
        .setInstances(4)
        .setConfig(vertx.getOrCreateContext().config());
      vertx.deployVerticle(WorkVerticle.class.getName(), workerDeploymentOptions);

      DeploymentOptions webDeploymentOptions = new DeploymentOptions()
        .setConfig(vertx.getOrCreateContext().config());
      vertx.deployVerticle(WebVerticle.class.getName(), webDeploymentOptions);
    };
    return runner;
  }

  private void toolInit() {
    Reflections reflections = ReflectionUtils.getReflection();
    Set<Method> methods = reflections.getMethodsAnnotatedWith(UtilsInital.class);
    for (Method method : methods) {
      method.setAccessible(true);
      try {
        Class<?>[] types = method.getParameterTypes();
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
        method.invoke(null, args);

      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
}
