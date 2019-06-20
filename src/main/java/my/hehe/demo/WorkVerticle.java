package my.hehe.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import my.hehe.demo.proxy.FilesCatcher;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.HashSet;
import java.util.Iterator;

public class WorkVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    try {
      FilesCatcher filesCatcher = FilesCatcher.create(vertx, config().getJsonObject("files"));
      startFuture.complete();
    } catch (Exception e) {
      startFuture.fail(e);
    }
  }
}
