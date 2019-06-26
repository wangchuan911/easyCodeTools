package my.hehe.demo.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncFlow {

  private Deque<Handler<AsyncResult<AsyncFlow>>> handlers = new LinkedList<>();
  private Map<Object, String> flowNamesMap = null;

  private Future next = Future.future();
  private String currentFlow = null;

  private Handler catchHandler = o -> {
    if (o instanceof Throwable) {
      ((Throwable) o).printStackTrace();
    } else
      System.out.println(o.toString());
  };
  private Handler<AsyncFlow> finalHandler = null;

  private AsyncFlow() {

  }

  public static AsyncFlow getInstance() {
    return new AsyncFlow();
  }

  public synchronized AsyncFlow then(Handler<AsyncResult<AsyncFlow>> handle) {
    handlers.addLast(handle);
    return this;
  }

  public synchronized AsyncFlow then(String name, Handler<AsyncResult<AsyncFlow>> handle) {
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
    Future current = this._next();
    if (current == null) return;
    current.complete(this);
  }

  public void fail(Throwable e) {
    if (this.currentFlow != null && this.currentFlow.length() > 0) {
      e = new Throwable(this.errorMsg(e.getMessage()), e);
    }
    if (this.catchHandler != null) {
      this.catchHandler.handle(e);
    } else {
      Future current = this._next();
      if (current == null) return;
      current.fail(e);
    }
    this.end();
  }

  public void fail(String var) {
    String error = (this.currentFlow != null && this.currentFlow.length() > 0) ? this.errorMsg(var) : var;
    if (this.catchHandler != null) {
      this.catchHandler.handle(error);
    } else {
      Future current = this._next();
      if (current == null) return;
      current.fail(error);
    }
    this.end();
  }

  private String errorMsg(String var) {
    return new StringBuilder("Handler[ ").append(currentFlow).append(" ] -> CAUSE[ ").append(var).append(" ]").toString();
  }

  private Future _next() {
    Handler h = handlers.pollFirst();
    if (this.flowNamesMap != null) {
      this.currentFlow = this.flowNamesMap.get(h);
    }
    if (h == null) {
      this.end();
      return null;
    }
    Future current = next.setHandler(h);
    next = Future.future();
    return current;
  }

  public void end() {
    handlers.clear();
    if (this.finalHandler != null) {
      this.finalHandler.handle(this);
      this.finalHandler = null;
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

  public static void main(String[] args) {
    AtomicInteger a = new AtomicInteger(0);
    AtomicInteger b = new AtomicInteger(0);
    try {
      AsyncFlow f = AsyncFlow.getInstance()
        .then("flow" + a.incrementAndGet(), asyncFlowAsyncResult -> {
          if (asyncFlowAsyncResult.succeeded()) {
            System.out.println(b.incrementAndGet());
            asyncFlowAsyncResult.result().next();
          }
        }).then("flow" + a.incrementAndGet(), asyncFlowAsyncResult -> {
          if (asyncFlowAsyncResult.succeeded()) {
            System.out.println(b.incrementAndGet());
            asyncFlowAsyncResult.result().next();
          }
        }).then("flow" + a.incrementAndGet(), asyncFlowAsyncResult -> {
          if (asyncFlowAsyncResult.succeeded()) {
            System.out.println(b.incrementAndGet());
            asyncFlowAsyncResult.result().fail("err");
          } else {
            asyncFlowAsyncResult.cause().printStackTrace();
          }
        }).then("flow" + a.incrementAndGet(), asyncFlowAsyncResult -> {
          if (asyncFlowAsyncResult.succeeded()) {
            System.out.println(b.incrementAndGet());
            asyncFlowAsyncResult.result().next();
          }
        }).then("flow" + a.incrementAndGet(), asyncFlowAsyncResult -> {
          if (asyncFlowAsyncResult.succeeded()) {
            System.out.println(b.incrementAndGet());
            asyncFlowAsyncResult.result().next();
          }
        }).then("flow" + a.incrementAndGet(), asyncFlowAsyncResult -> {
          if (asyncFlowAsyncResult.succeeded()) {
            System.out.println(b.incrementAndGet());
            asyncFlowAsyncResult.result().next();
          }
        }).catchThen(o -> {
          if (o instanceof Throwable) {
            ((Throwable) o).printStackTrace();
          } else
            System.out.println(o.toString());
        }).finalThen(asyncFlow -> {
          System.out.println("end!");
        });
      f.start();
    } catch (
      Exception e) {
      e.printStackTrace();
    }
  }
}
