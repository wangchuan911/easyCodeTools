package my.hehe.demo;

import io.vertx.core.VertxOptions;

public class Launcher extends io.vertx.core.Launcher {
  @Override
  public void beforeStartingVertx(VertxOptions options) {
    options.setWorkerPoolSize(4)
      .setMaxEventLoopExecuteTime(Long.MAX_VALUE);
    System.out.println("beforeStartingVertx");
  }
}
