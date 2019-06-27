package my.hehe.demo.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncFlow {
  //流程列队
  private Deque<Handler<AsyncFlow>> handlers = new LinkedList<>();
  //流程别名
  private Map<Object, String> flowNamesMap = null;
  //异步器执行器
  private Future next = Future.future();
  //当前环节名
  private String currentFlow = null;
  //catch 执行器
  static private Vertx vertx = null;

  private Handler catchHandler = o -> {
    if (o instanceof Throwable) {
      ((Throwable) o).printStackTrace();
    } else
      System.out.println(o.toString());
  };
  //finlly 执行器
  private Handler<AsyncFlow> finalHandler = null;
  //数据总线
  private Map busMap = null;

  private AsyncFlow() {

  }

  public static AsyncFlow getInstance() {
    return new AsyncFlow();
  }

  public synchronized AsyncFlow then(Handler<AsyncFlow> handle) {
    handlers.addLast(handle);
    return this;
  }

  public synchronized AsyncFlow then(String name, Handler<AsyncFlow> handle) {
    if (this.flowNamesMap == null) {
      this.flowNamesMap = new HashMap<>();
    }
    this.flowNamesMap.put(handle, name);
    this.handlers.addLast(handle);
    return this;
  }

  public void start() {
    this.next();
  }

  public void next() {
    Handler h = handlers.pollFirst();
    if (h == null) {
      this.end();
      return;
    }
    Future current = next.setHandler((Handler<AsyncResult<AsyncFlow>>) async -> {
      if (async.succeeded()) {
        try {
          h.handle(this);
        } catch (Throwable e) {
          this.fail(e);
        }
      } else {
        this.fail(async.cause());
      }
    });
    if (this.flowNamesMap != null) {
      this.currentFlow = this.flowNamesMap.get(h);
    }
    next = Future.future();
    System.out.println(new StringBuilder("start handler [").append(currentFlow).append("]"));
    current.complete(this);
  }

  public void nextBlocking() {
    Handler h = handlers.pollFirst();
    if (h == null) {
      this.end();
      return;
    }
    vertx.executeBlocking(future -> {
      if (this.flowNamesMap != null) {
        this.currentFlow = this.flowNamesMap.get(h);
      }
      System.out.println(new StringBuilder("start handler [").append(currentFlow).append("] --> ").append(Thread.currentThread()));
      future.complete();
    }, async -> {
      if (async.succeeded()) {
        try {
          h.handle(this);
        } catch (Throwable e) {
          this.fail(e);
        }
      } else {
        this.fail(async.cause());
      }
    });

  }

  public void fail(Throwable e) {
    if (this.currentFlow != null && this.currentFlow.length() > 0) {
      e = new Throwable(this.errorMsg(e.getMessage()), e);
    }
    if (this.catchHandler != null) {
      this.catchHandler.handle(e);
    }
    this.end();
  }

  public void fail(String var) {
    String error = (this.currentFlow != null && this.currentFlow.length() > 0) ? this.errorMsg(var) : var;
    if (this.catchHandler != null) {
      this.catchHandler.handle(error);
    }
    this.end();
  }

  private String errorMsg(String var) {
    return new StringBuilder("Handler[ ").append(currentFlow).append(" ] -> CAUSE[ ").append(var).append(" ]").toString();
  }


  public void end() {
    handlers.clear();
    if (this.finalHandler != null) {
      this.finalHandler.handle(this);
      this.finalHandler = null;
    }
    if (this.busMap != null) {
      this.busMap.clear();
      this.busMap = null;
    }
  }

  public AsyncFlow catchThen(Handler throwableHandler) {
    this.catchHandler = throwableHandler;
    return this;
  }

  public AsyncFlow finalThen(Handler<AsyncFlow> finalHandler) {
    this.finalHandler = finalHandler;
    return this;
  }

  public synchronized Map getParam() {
    if (this.busMap == null) this.busMap = new ConcurrentHashMap();
    return this.busMap;
  }

  public static void main(String[] args) {
    AtomicInteger a = new AtomicInteger(0);
    AtomicInteger b = new AtomicInteger(0);
    AsyncFlow.initUtil(Vertx.vertx(), null);
    try {
      AsyncFlow f = AsyncFlow.getInstance()
        .then("flow" + a.incrementAndGet(), flow -> {
          System.out.println(b.incrementAndGet());
          flow.nextBlocking();
        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.nextBlocking();

        }).then("flow" + a.incrementAndGet(), flow -> {
          System.out.println(b.incrementAndGet());
          String aa = null;
          aa.length();
          flow.nextBlocking();

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.nextBlocking();

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.nextBlocking();

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.nextBlocking();

        }).finalThen(asyncFlow -> {
          System.out.println("end!");
        });
      f.start();
    } catch (
      Exception e) {
      e.printStackTrace();
    }
  }

  @UtilsInital
  static void initUtil(Vertx vertx, JsonObject jsonObject) {
    try {
      AsyncFlow.vertx = vertx;
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
