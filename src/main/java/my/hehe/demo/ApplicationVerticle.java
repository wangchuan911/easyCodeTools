package my.hehe.demo;

import io.vertx.core.*;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import my.hehe.demo.common.JdbcUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
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
        // deploy multiple instances to avoid serializing requests.
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
    try {
      JDBCClient rimdbTest = null;
      {
        JsonObject dbConfig =  config().getJsonObject("data").getJsonObject("data3");
        rimdbTest = JDBCClient.createShared(vertx, dbConfig);
      }

      Map<String, JDBCClient> jdbcClients = null;
      {
        Field field = JdbcUtils.class.getDeclaredField("jdbcClients");
        field.setAccessible(true);
        Object value = field.get(null);
        if (value == null) {
          field.set(null, jdbcClients = new HashMap<>());
        } else {
          jdbcClients = (Map<String, JDBCClient>) value;
        }
      }
      jdbcClients.put("rimdbTest", rimdbTest);

    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  private String getStringValue(String val) {
    if (StringUtils.isNotEmpty(val) && !val.toUpperCase().equals("NULL")) {
      return val;
    } else {
      return null;
    }
  }
}
