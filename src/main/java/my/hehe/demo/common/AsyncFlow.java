package my.hehe.demo.common;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.annotation.UtilsInital;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class AsyncFlow {
  //流程列队
  private Deque<Handler<AsyncFlow>> handlers = new LinkedList<>();
  //流程别名
  private Map<Object, String> flowNamesMap = null;
  //当前环节名
  private String currentFlow = null;
  //catch 执行器
  static private Vertx vertx = null;

  private Throwable throwable = null;

  private Handler<AsyncFlow> catchHandler = asyncFlow -> {
    asyncFlow.getError().printStackTrace();
  };
  //finlly 执行器
  private Handler<AsyncFlow> finalHandler = null;
  //数据总线
  private Map busMap = null;

  //当前 future
  private Future currentFuture = null;

  private boolean isStarting = false;
  private boolean isComplete = false;
  private boolean isError = false;


  private AsyncFlow() {

  }

  public static AsyncFlow getInstance() {
    return new AsyncFlow();
  }

  public synchronized AsyncFlow then(Handler<AsyncFlow> handle) {
    this.undoAtStart();
    this.undoAtCompelete();
    this.handlers.addLast(handle);
    return this;
  }

  public synchronized AsyncFlow then(String name, Handler<AsyncFlow> handle) {
    this.undoAtStart();
    this.undoAtCompelete();
    if (this.flowNamesMap == null) {
      this.flowNamesMap = new HashMap<>();
    }
    this.flowNamesMap.put(handle, name);
    this.handlers.addLast(handle);
    return this;
  }

  //放在方法中防止重复调用
  private void undoAtCompelete() {
    if (isComplete) {
      throw new RuntimeException("flow is compelete!");
    }
  }

  private void undoAtStart() {
    if (isStarting) {
      throw new RuntimeException("flow is Starting!");
    }
  }

  public void start() {
    synchronized (this) {
      this.undoAtStart();
      this.undoAtCompelete();
      this.isStarting = true;
    }
    this.next();
  }

  public void next() {
    this.undoAtCompelete();
    if (this.currentFuture != null)
      this.currentFuture.complete();
    Handler h = handlers.pollFirst();
    if (h == null) {
      this.end();
      return;
    }
    vertx.executeBlocking(future -> {
      currentFuture = future;
      try {
        if (this.flowNamesMap != null) {
          this.currentFlow = this.flowNamesMap.get(h);
        }
        System.out.println(new StringBuilder("start handler [").append(currentFlow).append("] --> ").append(Thread.currentThread()));
        h.handle(this);
      } catch (Throwable e) {
        future.fail(e);
      }
    }, async -> {
      if (!async.succeeded()) {
        this.doFail(async.cause());
      }
    });

  }

  public void fail(Throwable e) {
    currentFuture.fail(e);
  }

  private void doFail(Throwable e) {
    try {
      this.isError = true;
      if (this.currentFlow != null && this.currentFlow.length() > 0) {
        String err = this.errorMsg(e);
        e = new Throwable(err, e);
      }
      if (this.catchHandler != null) {
        throwable = e;
        this.catchHandler.handle(this);
      }
    } finally {
      this.end();
    }
  }

  public void fail(String var) {
    this.fail(new Throwable(var));
  }

  private String errorMsg(Throwable var) {
    /*StringBuilder sb = new StringBuilder();
    {
      StackTraceElement[] stackTraceElements = var.getStackTrace();
      for (int i = 0; i < var.getStackTrace().length; i++) {
        sb.append(stackTraceElements[i].toString()).append('\n');
      }
    }*/
    String errorFormat = "流程[ %s ] 执行异常 [ %s ] 原因 [ %s ] 位置 { %s }";
    return String.format(errorFormat, this.currentFlow, var.getClass().getName(), var.getMessage(), (var.getClass() == Throwable.class ? var.getStackTrace()[1] : var.getStackTrace()[0]).toString());
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
    this.isComplete = true;
    this.currentFuture = null;
  }

  public AsyncFlow catchThen(Handler<AsyncFlow> throwableHandler) {
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

  public Throwable getError() {
    return this.throwable;
  }

  public boolean isComplete() {
    return isComplete;
  }

  public boolean isError() {
    return isError;
  }

  public static void main(String[] args) {
    AtomicInteger a = new AtomicInteger(0);
    AtomicInteger b = new AtomicInteger(0);
    AsyncFlow.initUtil(Vertx.vertx(), null);
    try {
      AsyncFlow f = AsyncFlow.getInstance()
        .then("flow" + a.incrementAndGet(), flow -> {
          System.out.println(b.incrementAndGet());
          flow.next();
        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();

        }).then("flow" + a.incrementAndGet(), flow -> {
          System.out.println(b.incrementAndGet());
          String aa = null;
//          aa.length();
//          flow.next();
          flow.fail("error");

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();

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
