package my.hehe.demo.common;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.*;

public class PromiseFlow {

	static Vertx vertx = Vertx.vertx();
	final PromiseFlow.FlowUnit head;
	int size;
	STATE state;
	Handler<PromiseFlow> finalHandler;
	Handler<Throwable> throwHandler;

	public PromiseFlow(Handler<FlowUnit> handle) {
		state = STATE.PREPARE;
		final PromiseFlow.FlowUnit flowUnit = new PromiseFlow.FlowUnit(this.size = 1, "start", handle);
		this.head = flowUnit;
	}

	void execute(PromiseFlow.FlowUnit flowUnit) {
		this.isRuning(true);
		if (flowUnit == null) {
			this.end(STATE.FINISH);
			return;
		}
		vertx.executeBlocking(promise -> {
			flowUnit.promise = promise;
			flowUnit.handle();
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				this.execute(flowUnit.nextUnit);
			} else {
				if (throwHandler != null)
					throwHandler.handle(asyncResult.cause());
				this.end(STATE.FAIL);
			}
		});
	}

	public PromiseFlow then(Handler<PromiseFlow.FlowUnit> handle) {
		return this.then(null, handle);
	}

	public PromiseFlow then(String name, Handler<PromiseFlow.FlowUnit> handle) {
		this.isPrepare(true);
		PromiseFlow.FlowUnit idxUnit = this.head;
		for (int i = 0; i < this.size - 1; i++) {
			idxUnit = idxUnit.nextUnit;
		}
		idxUnit.nextUnit = new PromiseFlow.FlowUnit(++this.size, name, handle);
		return this;
	}

	public PromiseFlow catchThen(Handler<Throwable> throwableHandler) {
		this.isPrepare(true);
		this.throwHandler = throwableHandler;
		return this;
	}

	public PromiseFlow finalThen(Handler<PromiseFlow> finalHandler) {
		this.isPrepare(true);
		this.finalHandler = finalHandler;
		return this;
	}

	void end(STATE state) {
		this.isRuning(true);
		this.state = state;
		if (this.finalHandler != null) {
			this.finalHandler.handle(this);
		}
//		vertx.close();
	}

	public void start() {
		this.isPrepare(true);
		state = STATE.RUNING;
		execute(this.head);
	}

	class FlowUnit {
		private int index;
		private String name;
		private Handler<PromiseFlow.FlowUnit> handler;
		private Promise promise;
		private Map<String, Object> param;
		private PromiseFlow.FlowUnit nextUnit;

		FlowUnit(int index, String name, Handler<PromiseFlow.FlowUnit> handle) {
			this.name = name;
			this.handler = handle;
			this.index = index;
		}

		public String getName() {
			return name;
		}

		void handle() {
			this.handler.handle(this);
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

		public <T> T getParam(String key, Class<T> t) {
			return (T) this.param.get(key);
		}

		public void setParam(String key, Object value) {
			if (this.param == null)
				this.param = new HashMap<>();
			this.param.put(key, value);
		}
	}

	public static void main(String[] args) {
		new PromiseFlow(flowUnit -> {
			System.out.println(flowUnit.toString());
			System.out.println(1);
			flowUnit.next();
		}).then(flowUnit -> {
			System.out.println(flowUnit.toString());
			System.out.println(2);
			flowUnit.next();
		}).then(flowUnit -> {
			System.out.println(flowUnit.toString());
			System.out.println(3);
			String a = null;
			a.toString();
		}).catchThen(throwable -> {
			throwable.printStackTrace();
		}).finalThen(promiseFlow -> {
			System.out.println("fin");
		}).start();

		new PromiseFlow(flowUnit -> {
			System.out.println(flowUnit.toString());
			System.out.println(1);
			flowUnit.next();
		}).then(flowUnit -> {
			System.out.println(flowUnit.toString());
			System.out.println(2);
			flowUnit.next();
		}).then(flowUnit -> {
			System.out.println(flowUnit.toString());
			System.out.println(3);
			String a = null;
			a.toString();
		}).catchThen(throwable -> {
			throwable.printStackTrace();
		}).finalThen(promiseFlow -> {
			System.out.println("fin");
		}).start();
	}

	enum STATE {
		PREPARE(0), RUNING(1), FINISH(2), FAIL(-1);
		int code;

		STATE(int stateCode) {
			this.code = stateCode;
		}
	}

	boolean isPrepare(boolean throwbale) {
		if (this.state.code != STATE.PREPARE.code) {
			if (throwbale) throw new RuntimeException("not Prepare");
			return false;
		}
		return true;
	}

	boolean isRuning(boolean throwbale) {
		if (this.state.code != STATE.RUNING.code) {
			if (throwbale) throw new RuntimeException("not Running");
			return false;
		}
		return true;
	}

	boolean isStop(boolean throwbale) {
		if (this.isPrepare(false) || this.isRuning(false)) {
			if (throwbale) throw new RuntimeException("not Stop");
			return false;
		}
		return true;
	}
}
