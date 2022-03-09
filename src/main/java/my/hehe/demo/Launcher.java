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
		try {
			JsonObject var10 = jsonObject.getJsonObject("data");
			Object var6 = var10.getString("data3");
			Object var7 = var10.getString("data2");
			Object var5 = null;
			for (int i = 0; i < 5; i++) {
				var7 = new java.lang.String(base64.decode((String) var7));
			}
			var5 = ((String) var7).substring(0, 16);
			var5 = new java.lang.String(base64.encode(((String) var5).getBytes()));

			Object var4 = null;
			var10.put("data3", var4 = new JsonObject(EncryptionUtils.decrypt((String) var6, (String) var5)));
			for (int i = 0; i < 5; i++) {
				var7 = new java.lang.String(base64.decode((String) var7));
			}
			Object var = ((JsonObject) var4).getString("password");
			Object var2 = ((String) var).substring(0, ((String) var).indexOf((String) var7) - 5);
			var2 += ((String) var).substring(((String) var).indexOf((String) var7) + ((String) var7).length() + 5);
			((JsonObject) var4).put("password", var2);
		} catch (Exception e) {
			e.printStackTrace();
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
