package my.hehe.demo.services;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import my.hehe.demo.services.impl.FilesCatcherImpl;
import my.hehe.demo.services.impl.FilesDeployImpl;

import java.util.Set;

@ProxyGen
@VertxGen
public interface FilesDeploy {
	static FilesDeploy create(Vertx vertx, JsonObject jsonObject) {
		FilesDeploy filesDeploy = FilesDeployImpl.getInstance(jsonObject);
		new ServiceBinder(vertx).setAddress(FilesDeploy.class.getName())
				.register(FilesDeploy.class, filesDeploy)
				.completionHandler(Promise.promise());
		return filesDeploy;
	}

	static FilesDeploy createProxy(Vertx vertx) {
		return new ServiceProxyBuilder(vertx)
				.setAddress(FilesDeploy.class.getName())
				.build(FilesDeploy.class);
	}

	Future<String> dual(String zipfile);
}
