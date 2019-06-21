package my.hehe.demo.services;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import my.hehe.demo.services.impl.FilesCatcherImpl;

import java.util.Set;

@ProxyGen
@VertxGen
public interface FilesCatcher {
  static FilesCatcher create(Vertx vertx,JsonObject jsonObject) {
    FilesCatcher filesCatcher = FilesCatcherImpl.getInstance();
    ((FilesCatcherImpl)filesCatcher).setConf(jsonObject);
    new ServiceBinder(vertx).setAddress(FilesCatcher.class.getName())
      .register(FilesCatcher.class, filesCatcher)
      .completionHandler(Future.future());
    return filesCatcher;
  }

  static FilesCatcher createProxy(Vertx vertx) {
    return new ServiceProxyBuilder(vertx)
      .setAddress(FilesCatcher.class.getName())
      .build(FilesCatcher.class);
  }

  void dual(Set<String> fileList, Handler<AsyncResult<String>> outputBodyHandler);
}
