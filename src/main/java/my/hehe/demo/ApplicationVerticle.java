package my.hehe.demo;

import io.vertx.core.*;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.json.JsonObject;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.function.Consumer;

public class ApplicationVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    deploy().accept(vertx);
  }

  private static Consumer<Vertx> deploy() {
    Consumer<Vertx> runner = vertx -> {
      DeploymentOptions workerDeploymentOptions = new DeploymentOptions()
        .setWorker(true)
        // As worker verticles are never executed concurrently by Vert.x by more than one thread,
        // deploy multiple instances to avoid serializing requests.
        .setInstances(4)
        .setConfig(vertx.getOrCreateContext().config());
      vertx.deployVerticle(WorkVerticle.class.getName(), workerDeploymentOptions);


      vertx.deployVerticle(WebVerticle.class.getName());
    };
    return runner;
  }
}
