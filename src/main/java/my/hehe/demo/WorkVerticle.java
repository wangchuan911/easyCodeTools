package my.hehe.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import my.hehe.demo.services.FilesCatcher;
import my.hehe.demo.services.FilesDeploy;

public class WorkVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    try {
      FilesCatcher filesCatcher = FilesCatcher.create(vertx, config().getJsonObject("files"));
      FilesDeploy filesDeploy = FilesDeploy.create(vertx, config().getJsonObject("files"));
      startFuture.complete();
    } catch (Exception e) {
      startFuture.fail(e);
    }
  }
}
