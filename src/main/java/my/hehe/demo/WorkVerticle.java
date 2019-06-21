package my.hehe.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import my.hehe.demo.services.FilesCatcher;

public class WorkVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    try {
      FilesCatcher filesCatcher = FilesCatcher.create(vertx, config());
      startFuture.complete();
    } catch (Exception e) {
      startFuture.fail(e);
    }
  }
}
