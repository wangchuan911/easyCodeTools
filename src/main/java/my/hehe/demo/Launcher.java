package my.hehe.demo;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.encrypt.EncryptionUtils;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Launcher extends VertxCommandLauncher implements VertxLifecycleHooks {

  static Charset CHARSET = StandardCharsets.UTF_8;
  static org.apache.commons.codec.binary.Base64 base64 = new Base64();
  public static void main(String[] args) {
    (new Launcher()).dispatch(args);
  }

  public static void executeCommand(String cmd, String... args) {
    (new Launcher()).execute(cmd, args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    options.setWorkerPoolSize(4)
      .setMaxEventLoopExecuteTime(Long.MAX_VALUE);
  }

  @Override
  public void afterConfigParsed(JsonObject jsonObject) {
    System.out.println(jsonObject);
    JsonObject dataJsonObject = jsonObject.getJsonObject("data");
    String data = dataJsonObject.getString("data3");
    String key = dataJsonObject.getString("data2");
    for (int i = 0; i < 5; i++) {
      key = new java.lang.String(base64.decode(key));
    }
    key=key.substring(0,16);
    key=new java.lang.String(base64.encode(key.getBytes()));
    try {
      dataJsonObject.put("data3",EncryptionUtils.decrypt(data,key));
    }catch (Exception e){

    }
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
