package my.hehe.demo.common;

import io.vertx.core.*;
import io.vertx.core.impl.future.CompositeFutureImpl;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Function;

public class PromiseFlow {

	static Vertx vertx = Vertx.vertx();
	final PromiseFlow.FlowUnit head = new PromiseFlow.FlowUnit(this.size = 0, null, null, null);
	int size;
	SuccessHandler success, complete;
	FailHandler fail;

	public PromiseFlow(String name, ResolveHandler resolve, RejectAndRetryHandler reject) {
		this.head.resolve = resolve;
		this.head.reject = reject;
		this.head.name = StringUtils.isEmpty(name) ? String.format("Step %d:%s", this.size, Thread.currentThread().getStackTrace()[1]) : name;
		this.size = ++this.head.index;
	}

	public PromiseFlow(String name, ResolveHandler resolve) {
		this(name, resolve, null);
	}

	public PromiseFlow(ResolveHandler resolve, RejectAndRetryHandler reject) {
		this(null, resolve, reject);
	}

	public PromiseFlow(ResolveHandler handle) {
		this(null, handle, null);
	}

	public PromiseFlow then(ResolveHandler resolve, RejectAndRetryHandler reject) {
		return this.then(null, resolve, reject);
	}

	public PromiseFlow then(ResolveHandler resolve) {
		return this.then(null, resolve, null);
	}

	public PromiseFlow then(String name, ResolveHandler resolve) {
		return this.then(name, resolve, null);
	}

	public PromiseFlow then(String name, ResolveHandler resolve, RejectAndRetryHandler reject) {
		PromiseFlow.FlowUnit idxUnit = this.head;
		for (int i = 0; i < this.size - 1; i++) {
			idxUnit = idxUnit.nextUnit;
		}
		if (this.size == 0) {
			idxUnit.resolve = resolve;
			idxUnit.name = StringUtils.isEmpty(name) ? String.format("Step %d:%s", this.size, Thread.currentThread().getStackTrace()[1]) : name;
			idxUnit.index = ++this.size;
		} else {
			idxUnit.nextUnit = new PromiseFlow.FlowUnit(++this.size, name, resolve, reject);
		}
		return this;
	}

	public PromiseFlow allThen(String name, PromiseFlow... promiseFlows) {
		return this.then(name, flowUnitState -> {
			switch (promiseFlows.length) {
				case 0:
					flowUnitState.next();
					break;
				case 1:
					promiseFlows[0].start(flowUnitState.param, flowEndUnitState -> {
						flowUnitState.next();
					}, throwable -> {
						flowUnitState.fail(throwable.cause());
					});
					break;
				default:
					CompositeFutureImpl
							.all(Arrays
									.stream(promiseFlows)
									.map(promiseFlow ->
											Future.future(promise1 -> {
												promiseFlow.start(flowUnitState.param, flowEndUnitState -> {
													promise1.complete();
												}, throwable -> {
													promise1.fail(throwable.cause());
												});
											})).toArray(Future[]::new))
							.onSuccess(compositeFuture -> {
								flowUnitState.next();
							})
							.onFailure(throwable -> {
								flowUnitState.fail(throwable);
							});
			}
		});
	}

	public PromiseFlow switchThen(String name, Function<FlowUnitState, Integer> stateFunction, PromiseFlow... promiseFlows) {
		return this.then(name, flowUnitState -> {
			promiseFlows[stateFunction.apply(flowUnitState)]
					.start(flowUnitState.param, flowEndUnitState -> {
						flowUnitState.next();
					}, throwable -> {
						flowUnitState.fail(throwable.cause());
					});
		});
	}


	public PromiseFlow fail(FailHandler fail) {
		this.fail = fail;
		return this;
	}

	public PromiseFlow complete(SuccessHandler complete) {
		this.complete = complete;
		return this;
	}

	public PromiseFlow success(SuccessHandler success) {
		this.success = success;
		return this;
	}

	public PromiseFlow.Flow start() {
		return start(null);
	}

	public PromiseFlow.Flow start(Map<String, Object> params) {
		PromiseFlow.Flow flow = new PromiseFlow.Flow(this.head, this.success, this.fail, this.complete);
		flow.start(params);
		return flow;
	}

	public PromiseFlow.Flow start(Map<String, Object> params, Handler<FlowEndUnitState> success, FailHandler fail) {
		return this.start(params, success, fail, null);
	}

	public PromiseFlow.Flow start(Map<String, Object> params, Handler<FlowEndUnitState> success, FailHandler fail, Handler<FlowEndUnitState> complete) {
		PromiseFlow.Flow flow = new PromiseFlow.Flow(this.head, flowEndUnitState -> {
			if (this.success != null) {
				this.success.handle(flowEndUnitState);
			}
			if (success != null) {
				success.handle(flowEndUnitState);
			}
		}, throwable -> {
			if (this.fail != null) {
				this.fail.handle(throwable);
			}
			if (fail != null) {
				fail.handle(throwable);
			}
		}, flowEndUnitState -> {
			if (this.complete != null) {
				this.complete.handle(flowEndUnitState);
			}
			if (complete != null) {
				complete.handle(flowEndUnitState);
			}
		});
		flow.start(params);
		return flow;
	}


	public class Flow {
		PromiseFlow.FlowUnit head;
		STATE state = STATE.PREPARE;
		SuccessHandler success, complete;
		FailHandler fail;

		public Flow(FlowUnit head, SuccessHandler success, FailHandler fail, SuccessHandler complete) {
			this.head = head;
			this.success = success;
			this.complete = complete;
			this.fail = fail;
		}

		void execute(final PromiseFlow.FlowUnit flowUnit, final Map<String, Object> params) {
			this.isRuning(true);
			if (flowUnit == null) {
				this.end(STATE.FINISH, params);
				return;
			}
			vertx.executeBlocking(promise -> {
				try {
					flowUnit.handle(new FlowUnitState(promise, params));
				} catch (Throwable throwable) {
					promise.fail(throwable);
				}
			}, asyncResult -> {
				if (asyncResult.succeeded()) {
					this.execute(flowUnit.nextUnit, params);
				} else {
					if (flowUnit.reject != null) {
						vertx.executeBlocking(promise -> {
							try {
								flowUnit.reject.handle(new FlowRejectUnitState(promise, params, asyncResult.cause()));
							} catch (Throwable e) {
								promise.fail(e);
							}
						}, asyncResult1 -> {
							if (asyncResult1.succeeded()) {
								this.execute(flowUnit.nextUnit, params);
							} else {
								if (this.fail != null)
									this.fail.handle(new FlowFailEndUnitState(params, asyncResult1.cause()));
								this.end(STATE.FAIL, params);
							}
						});
					} else {
						if (this.fail != null)
							this.fail.handle(new FlowFailEndUnitState(params, asyncResult.cause()));
						this.end(STATE.FAIL, params);
					}
				}
			});
		}

		void end(STATE state, Map<String, Object> params) {
			this.isRuning(true);
			this.state = state;
			FlowEndUnitState complete = new FlowEndUnitState(params);
			switch (state) {
				case FINISH:
					if (this.success != null) {
						this.success.handle(complete);
					}
					break;
				case FAIL:
					complete.isFail = true;
					break;
			}
			if (this.complete != null) {
				this.complete.handle(complete);
			}
//		vertx.close();
		}

		void start() {
			this.start(null);
		}

		void start(Map<String, Object> params) {
			this.isPrepare(true);
			state = STATE.RUNING;
			execute(this.head, params == null ? new HashMap<>() : params);
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

	public class FlowEndUnitState extends FlowUnitState {
		boolean isFail;

		FlowEndUnitState(Map<String, Object> param) {
			super(null, param);
		}

		public boolean isFail() {
			return isFail;
		}

		@Override
		public void next() {
			throw new RuntimeException("is complete");
		}

		@Override
		public void fail(String s) {
			throw new RuntimeException("is complete");
		}

		@Override
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

	public final class FlowFailEndUnitState extends FlowEndUnitState {
		Throwable throwable;

		FlowFailEndUnitState(Map<String, Object> param, Throwable throwable) {
			super(param);
			this.throwable = throwable;
		}

		public Throwable cause() {
			return this.throwable;
		}
	}

	public class FlowRejectUnitState extends FlowUnitState {
		Throwable throwable;

		FlowRejectUnitState(Promise promise, Map<String, Object> param, Throwable throwable) {
			super(promise, param);
			this.throwable = throwable;
		}

		public Throwable cause() {
			return this.throwable;
		}

	}

	class FlowUnit {
		int index;
		String name;
		ResolveHandler resolve;
		RejectAndRetryHandler reject;
		PromiseFlow.FlowUnit nextUnit;

		FlowUnit(int index, String name, ResolveHandler resolve, RejectAndRetryHandler reject) {
			this.name = name;
			this.resolve = resolve;
			this.reject = reject;
			this.index = index;
		}

		public String getName() {
			return name;
		}

		void handle(FlowUnitState flowUnitState) throws Throwable {
			this.resolve.handle(flowUnitState);
		}


	}

	static PromiseFlow f = new PromiseFlow(flowUnit -> {
		System.out.println(flowUnit.toString());
		System.out.println(1);
		flowUnit.next();
	}).then(flowUnit -> {
		System.out.println(flowUnit.toString());
		System.out.println(2);
		/*String a = null;
		a.toString();*/
		flowUnit.next();
	}, (flowUnitState) -> {
		System.out.println("skip");
		flowUnitState.cause().printStackTrace();
		flowUnitState.next();
	}).then(flowUnit -> {
		System.out.println(flowUnit.toString());
		System.out.println(3);
		flowUnit.next();
	}).fail(throwable -> {
		throwable.cause().printStackTrace();
	}).complete(promiseFlow -> {
		System.out.println("fin");
	});

	public static void main(String[] args) {
		System.out.println("start");
		f.start();

//		f.start();
		System.out.println("end");
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		System.out.println(uuid);
		System.out.println(uuid.length());

	}

	enum STATE {
		PREPARE(0), RUNING(1), FINISH(2), FAIL(-1);
		int code;

		STATE(int stateCode) {
			this.code = stateCode;
		}
	}

	@FunctionalInterface
	public interface RejectAndRetryHandler {
		void handle(FlowRejectUnitState flowRejectUnitState) throws Throwable;
	}

	@FunctionalInterface
	public interface ResolveHandler {
		void handle(PromiseFlow.FlowUnitState flowUnitState) throws Throwable;
	}

	@FunctionalInterface
	public interface SuccessHandler {
		void handle(PromiseFlow.FlowEndUnitState flowUnitState);
	}

	@FunctionalInterface
	public interface FailHandler {
		void handle(PromiseFlow.FlowFailEndUnitState flowUnitState);
	}
}

