package my.hehe.demo.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.Map;

public class JdbcUtils {
  private static Map<String, JDBCClient> jdbcClients = null;

  public static void getConnect(String name, Handler<AsyncResult<SQLConnection>> var1) {
    jdbcClients.get(name).getConnection(var1);
  }

  public static JDBCClient getJdbcClient(String name) {
    return jdbcClients.get(name);
  }
}
