package my.hehe.demo.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import my.hehe.demo.common.annotation.UtilsInital;

import java.util.HashMap;
import java.util.Map;

public class JdbcUtils {
  private static Map<String, JDBCClient> jdbcClients = null;

  public static void getConnect(String name, Handler<AsyncResult<SQLConnection>> var1) {
    jdbcClients.get(name).getConnection(var1);
  }

  public static JDBCClient getJdbcClient(String name) {
    return jdbcClients.get(name);
  }

  @UtilsInital
  JdbcUtils(Vertx vertx, JsonObject jsonObject) {
    try {
      JDBCClient rimdbTest = null;
      {
        JsonObject dbConfig = jsonObject.getJsonObject("data").getJsonObject("data3");
        rimdbTest = JDBCClient.createShared(vertx, dbConfig);
      }
      {
        if (jdbcClients == null) {
          jdbcClients = new HashMap<>();
        }
      }
      jdbcClients.put("rimdbTest", rimdbTest);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
