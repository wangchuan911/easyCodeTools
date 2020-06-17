package my.hehe.demo.common;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.*;

public class PromiseFlow {

	static Vertx vertx = Vertx.vertx();
	final PromiseFlow.FlowUnit head = new PromiseFlow.FlowUnit(this.size = 0, "start", null);
	int size;
	STATE state = STATE.PREPARE;
	Handler<FlowEndUnitState> finalHandler;
	Handler<Throwable> throwHandler;

	public PromiseFlow(Handler<FlowUnitState> handle) {
		this.head.handler = handle;
		this.size = ++this.head.index;
	}

	public PromiseFlow() {
	}

	void execute(PromiseFlow.FlowUnit flowUnit, Map<String, Object> params) {
		this.isRuning(true);
		if (flowUnit == null) {
			this.end(STATE.FINISH, params);
			return;
		}
		vertx.executeBlocking(promise -> {
			flowUnit.handle(new FlowUnitState(promise, params));
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				this.execute(flowUnit.nextUnit, params);
			} else {
				if (throwHandler != null)
					throwHandler.handle(asyncResult.cause());
				this.end(STATE.FAIL, params);
			}
		});
	}

	public PromiseFlow then(Handler<FlowUnitState> handle) {
		return this.then(null, handle);
	}

	public PromiseFlow then(String name, Handler<FlowUnitState> handle) {
		this.isPrepare(true);
		PromiseFlow.FlowUnit idxUnit = this.head;
		for (int i = 0; i < this.size - 1; i++) {
			idxUnit = idxUnit.nextUnit;
		}
		if (this.size == 0) {
			idxUnit.handler = handle;
			idxUnit.name = name;
			idxUnit.index = ++this.size;
		} else {
			idxUnit.nextUnit = new PromiseFlow.FlowUnit(++this.size, name, handle);
		}
		return this;
	}

	public PromiseFlow catchThen(Handler<Throwable> throwableHandler) {
		this.isPrepare(true);
		this.throwHandler = throwableHandler;
		return this;
	}

	public PromiseFlow finalThen(Handler<FlowEndUnitState> finalHandler) {
		this.isPrepare(true);
		this.finalHandler = finalHandler;
		return this;
	}

	void end(STATE state, Map<String, Object> params) {
		this.isRuning(true);
		this.state = state;
		if (this.finalHandler != null) {
			this.finalHandler.handle(new FlowEndUnitState(params));
		}
//		vertx.close();
	}

	public void start() {
		this.start(null);
	}

	public void start(Map<String, Object> params) {
		this.isPrepare(true);
		state = STATE.RUNING;
		execute(this.head, params == null ? new HashMap<>() : params);
	}

	public final class FlowEndUnitState extends FlowUnitState {
		FlowEndUnitState(Map<String, Object> param) {
			super(null, param);
		}

		public void next() {
			throw new RuntimeException("is complete");
		}

		public void fail(String s) {
			throw new RuntimeException("is complete");
		}

		public void fail(Throwable t) {
			throw new RuntimeException("is complete");
		}
	}

	public class FlowUnitState {
		Promise promise;
		Map<String, Object> param;

		FlowUnitState(Promise promise, Map<String, Object> param) {
			this.promise = promise;
			this.param = param;
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

	class FlowUnit {
		int index;
		String name;
		Handler<FlowUnitState> handler;
		PromiseFlow.FlowUnit nextUnit;

		FlowUnit(int index, String name, Handler<FlowUnitState> handle) {
			this.name = name;
			this.handler = handle;
			this.index = index;
		}

		public String getName() {
			return name;
		}

		void handle(FlowUnitState flowUnitState) {
			this.handler.handle(flowUnitState);
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

		new PromiseFlow().then(flowUnit -> {
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

	public boolean isComplete() {
		return this.state == STATE.FINISH;
	}

	public boolean isError() {
		return this.state == STATE.FAIL;
	}
}