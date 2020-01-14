package my.hehe.demo.common;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.annotation.UtilsInital;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncFlow {

  private Deque<FlowUnit> handlers = new LinkedList<>();

  private FlowUnit flowUnitNow = null;

  static private Vertx vertx = null;

  private Throwable throwable = null;

  private Handler<Throwable> catchHandler = STATIC_CATCH_HANDLER;
  //finlly 执行器
  private Handler<AsyncFlow> finalHandler = null;
  //数据总线
  private Map<String, Object> busData = new HashMap(4);

  private int state = INITAIL;

  static private final int STARTING = 0;
  static private final int COMPLETE = 1;
  static private final int ERROR = -1;
  static private final int INITAIL = -2;
  static private final Handler<Throwable> STATIC_CATCH_HANDLER = throwableFlow -> {
    throwableFlow.printStackTrace();
  };
  static private final String FORMATE = "start handler [%d:%s] -->%s";
  static private final String ERROR_FORMAT = "流程[ %s ] 执行异常 [ %s ] 原因 [ %s ] 位置 { %s }";


  private AsyncFlow() {

  }

  public static AsyncFlow getInstance() {
    return new AsyncFlow();
  }

  public synchronized AsyncFlow then(Handler<FlowUnit> handle) {
    return then(null, handle);
  }

  public synchronized AsyncFlow then(String name, Handler<FlowUnit> handle) {
    this.undoAtStart();
    this.undoAtCompelete();
    this.handlers.addLast(new FlowUnit().setOrder(handlers.size()).setHandler(handle).setName(name));
    return this;
  }

  //放在方法中防止重复调用
  private void undoAtCompelete() {
    if (this.state == COMPLETE) {
      throw new RuntimeException("flow is compelete!");
    }
  }

  private void undoAtStart() {
    if (this.state == STARTING) {
      throw new RuntimeException("flow is Starting!");
    }
  }

  public void start() {
    synchronized (this) {
      this.undoAtStart();
      this.undoAtCompelete();
      this.state = STARTING;
    }
    this.next();
  }


  private void next() {
    if (this.state == ERROR) return;
    this.undoAtCompelete();
    this.flowUnitNow = handlers.pollFirst();
    if (flowUnitNow == null) {
      this.end();
      return;
    }
    vertx.executeBlocking(promise -> {
      this.flowUnitNow.setPromise(promise);
      this.flowUnitNow.setParam(busData);
      try {
        System.out.println(String.format(FORMATE, this.flowUnitNow.getOrder(), this.flowUnitNow.getName(), Thread.currentThread().toString()));
        this.flowUnitNow.handle();
      } catch (Throwable e) {
        this.flowUnitNow.fail(e);
      }
    }, async -> {
      if (!async.succeeded()) {
        this.doFail(async.cause());
      } else {
        this.next();
      }
    });
  }


  private void doFail(Throwable e) {
    try {
      this.state = ERROR;
      if (StringUtils.isNotEmpty(this.flowUnitNow.getName())) {
        String err = this.errorMsg(e);
        e = new Throwable(err, e);
      }
      if (this.catchHandler != null) {
        throwable = e;
        this.catchHandler.handle(e);
      }
    } finally {
      this.end();
    }
  }

  private String errorMsg(Throwable var) {
    return String.format(ERROR_FORMAT, this.flowUnitNow.getName(), var.getClass().getName(), var.getMessage(), (var.getClass() == Throwable.class ? var.getStackTrace()[1] : var.getStackTrace()[0]).toString());
  }


  public void end() {
    if (this.finalHandler != null) {
      this.finalHandler.handle(this);
      this.finalHandler = null;
    }
    if (this.busData != null) {
      this.busData.clear();
      this.busData = null;
    }
    this.state = COMPLETE;
    this.flowUnitNow = null;
  }

  public AsyncFlow catchThen(Handler<Throwable> throwableHandler) {
    this.catchHandler = throwableHandler;
    return this;
  }

  public AsyncFlow finalThen(Handler<AsyncFlow> finalHandler) {
    this.finalHandler = finalHandler;
    return this;
  }

  public synchronized Map getParam() {
    if (this.busData == null) this.busData = new ConcurrentHashMap();
    return this.busData;
  }

  public Throwable getError() {
    return this.throwable;
  }

  public boolean isComplete() {
    return this.state == COMPLETE;
  }

  public boolean isError() {
    return this.state == ERROR;
  }

  public static void main(String[] args) {

 /*   List<Integer> integers = new ArrayList<>();
    integers.add(1);
    integers.add(3);
    integers.add(2);
    Collections.sort(integers, new Comparator<Integer>() {
      public int compare(Integer o1, Integer o2) {
        return (new Integer(o1)).compareTo(new Integer(o2));
      }
    });
    System.out.println(integers);


    System.out.println("aaa$12".matches("aaa"+"\\$[0-9]+"));*/


    /*String text = "<-------------------------------start---------------------------------->";

    Pattern p = Pattern.compile("\\<\\w+\\>.+\\<\\/\\w+\\>");
    p = Pattern.compile("(\\<(\\/)?\\w+\\>)");
    p = Pattern.compile("(\\.)+start(\\.)+");
    System.out.println(p.matcher(text).matches());
    p = Pattern.compile("(-){10,}");
    List<String> a1 = Arrays.asList(p.split(text));
    System.out.println(a1);
   */

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
          aa.indexOf(1);
          flow.next();
//          flow.fail("error");

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();

        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();
        }).then("flow" + a.incrementAndGet(), flow -> {

          System.out.println(b.incrementAndGet());
          flow.next();

        }).catchThen(asyncFlow -> {

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

  public static class FlowUnit {
    private int order;
    private String name;
    private Handler<FlowUnit> handler;
    private Promise promise;
    private Map<String, Object> param;

    private FlowUnit() {
    }

    public int getOrder() {
      return order;
    }

    public FlowUnit setOrder(int order) {
      this.order = order;
      return this;
    }

    public String getName() {
      return name;
    }

    public FlowUnit setName(String name) {
      this.name = name;
      return this;
    }

    void handle() {
      this.handler.handle(this);
    }

    FlowUnit setHandler(Handler<FlowUnit> handler) {
      this.handler = handler;
      return this;
    }

    void setPromise(Promise promise) {
      this.promise = promise;
    }

    public void next() {
      this.promise.complete();
    }

    public void fail(String s) {
      this.promise.fail(s);
    }

    public void fail(Throwable t) {
      this.promise.fail(t);
    }

    void setParam(Map param) {
      this.param = param;
    }

    public synchronized <T> T getParam(String key, Class<T> t) {
      return (T) this.param.get(key);
    }

    public void setParam(String key, Object value) {
      this.param.put(key, value);
    }
  }
}
