package my.hehe.demo;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;

public class Launcher extends VertxCommandLauncher implements VertxLifecycleHooks {
  public static void main(String[] args) {
    (new Launcher()).dispatch(args);
  }

  public static void executeCommand(String cmd, String... args) {
    (new io.vertx.core.Launcher()).execute(cmd, args);
  }
  @Override
  public void beforeStartingVertx(VertxOptions options) {
    options.setWorkerPoolSize(4)
      .setMaxEventLoopExecuteTime(Long.MAX_VALUE);
    System.out.println("beforeStartingVertx");
  }

  @Override
  public void afterConfigParsed(JsonObject jsonObject) {

  }

  @Override
  public void afterStartingVertx(Vertx vertx) {

  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {

  }

  @Override
  public void beforeStoppingVertx(Vertx vertx) {

  }

  @Override
  public void afterStoppingVertx() {

  }

  @Override
  public void handleDeployFailed(Vertx vertx, String s, DeploymentOptions deploymentOptions, Throwable throwable) {

  }
}
