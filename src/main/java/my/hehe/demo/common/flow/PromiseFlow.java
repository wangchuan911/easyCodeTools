package my.hehe.demo.common.flow;

import io.vertx.core.*;
import io.vertx.core.impl.future.CompositeFutureImpl;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Function;

public class PromiseFlow extends AbstractFlow<PromiseFlow> {
	static Vertx vertx = Vertx.vertx();
	final FlowUnit head = new FlowUnit(this.size = 0, null, null, null);
	int size;
	SuccessHandler success;
	CompleteHandler complete;
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

	@Override
	public PromiseFlow then(ResolveHandler resolve, RejectAndRetryHandler reject) {
		return this.then(null, resolve, reject);
	}

	@Override
	public PromiseFlow then(ResolveHandler resolve) {
		return this.then(null, resolve, null);
	}

	@Override
	public PromiseFlow then(String name, ResolveHandler resolve) {
		return this.then(name, resolve, null);
	}

	@Override
	public PromiseFlow then(String name, ResolveHandler resolve, RejectAndRetryHandler reject) {
		FlowUnit idxUnit = this.head;
		for (int i = 0; i < this.size - 1; i++) {
			idxUnit = idxUnit.nextUnit;
		}
		if (this.size == 0) {
			idxUnit.resolve = resolve;
			idxUnit.name = StringUtils.isEmpty(name) ? String.format("Step %d:%s", this.size, Thread.currentThread().getStackTrace()[1]) : name;
			idxUnit.index = ++this.size;
		} else {
			idxUnit.nextUnit = new FlowUnit(++this.size, name, resolve, reject);
		}
		return this;
	}

	@Override
	public PromiseFlow allThen(String name, PromiseFlow... promiseFlows) {
		return this.then(name, flowUnitState -> {
			switch (promiseFlows.length) {
				case 0:
					flowUnitState.next();
					break;
				case 1:
					promiseFlows[0].start(((FlowUnitState) flowUnitState).param, flowEndUnitState -> {
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
												promiseFlow.start(((FlowUnitState) flowUnitState).param, flowEndUnitState -> {
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

	@Override
	public PromiseFlow switchThen(String name, SwitchHandler stateFunction, PromiseFlow... promiseFlows) {
		return this.then(name, flowUnitState -> {
			promiseFlows[stateFunction.apply(flowUnitState)]
					.start(((FlowUnitState) flowUnitState).param, flowEndUnitState -> {
						flowUnitState.next();
					}, throwable -> {
						flowUnitState.fail(throwable.cause());
					});
		});
	}


	@Override
	public PromiseFlow fail(FailHandler fail) {
		this.fail = fail;
		return this;
	}

	@Override
	public PromiseFlow complete(CompleteHandler complete) {
		this.complete = complete;
		return this;
	}

	@Override
	public PromiseFlow success(SuccessHandler success) {
		this.success = success;
		return this;
	}

	@Override
	public FlowStream start() {
		return start(null);
	}

	@Override
	public FlowStream start(Map<String, Object> params) {
		FlowStream flow = new FlowStream(this.head, this.success, this.fail, this.complete);
		flow.start(params);
		return flow;
	}

	@Override
	public FlowStream start(Map<String, Object> params, Handler<IFlowEndUnitState> success, FailHandler fail) {
		return this.start(params, success, fail, null);
	}

	@Override
	public FlowStream start(Map<String, Object> params, Handler<IFlowEndUnitState> success, FailHandler fail, Handler<IFlowEndUnitState> complete) {
		FlowStream flow = new FlowStream(this.head, flowEndUnitState -> {
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


	public class FlowStream extends AbstractFlowStream {


		public FlowStream(FlowUnit head, SuccessHandler success, FailHandler fail, CompleteHandler complete) {
			super(head, success, fail, complete);
		}

		@Override
		void execute(final FlowUnit flowUnit, final Map<String, Object> params) {
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

		@Override
		Function<Map<String, Object>, IFlowEndUnitState> getFlowEndUnitState() {
			return map -> new FlowEndUnitState(map);
		}
	}

	public class FlowEndUnitState extends FlowUnitState implements IFlowEndUnitState {
		boolean isFail;

		FlowEndUnitState(Map<String, Object> param) {
			super(null, param);
		}

		@Override
		public boolean isFail() {
			return isFail;
		}

		@Override
		public void flowFail() {
			this.isFail = true;
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

	public class FlowUnitState extends AbstractFlowUnitState {
		Promise promise;

		FlowUnitState(Promise promise, Map<String, Object> param) {
			this.promise = promise;
			this.param = param;
		}

		@Override
		public void next() {
			this.promise.complete();
		}

		@Override
		public void fail(String s) {
			this.promise.fail(s);
		}

		@Override
		public void fail(Throwable t) {
			this.promise.fail(t);
		}

	}

	public final class FlowFailEndUnitState extends FlowEndUnitState implements IFlowFailEndUnitState {
		Throwable throwable;

		FlowFailEndUnitState(Map<String, Object> param, Throwable throwable) {
			super(param);
			this.throwable = throwable;
		}

		@Override
		public Throwable cause() {
			return this.throwable;
		}
	}

	public class FlowRejectUnitState extends FlowUnitState implements IFlowRejectUnitState {
		Throwable throwable;

		FlowRejectUnitState(Promise promise, Map<String, Object> param, Throwable throwable) {
			super(promise, param);
			this.throwable = throwable;
		}

		@Override
		public Throwable cause() {
			return this.throwable;
		}

	}


	static PromiseFlow f = new PromiseFlow(flowUnit -> {
		//System.out.println(flowUnit.toString());
		String hehe = flowUnit.getParam("hehe");
		System.out.println("1:" + hehe);
		flowUnit.setParam("hehe", hehe + "->" + flowUnit.toString());
		flowUnit.next();
	}).then(flowUnit -> {
		//System.out.println(flowUnit.toString());
		String hehe = flowUnit.getParam("hehe");
		System.out.println("2:" + hehe);
		flowUnit.setParam("hehe", hehe + "->" + flowUnit.toString());
		String a = null;
//		a.toString();
		flowUnit.next();
	}/*, (flowUnitState) -> {
		String hehe = flowUnitState.getParam("hehe", String.class);
		System.out.println("skip:" + hehe);
		flowUnitState.setParam("hehe", hehe + "->" + flowUnitState.toString());
		flowUnitState.cause().printStackTrace();
		flowUnitState.next();
	}*/).then(flowUnit -> {
		//System.out.println(flowUnit.toString());
		String hehe = flowUnit.getParam("hehe");
		System.out.println("3:" + hehe);
		flowUnit.setParam("hehe", hehe + "->" + flowUnit.toString());
		flowUnit.next();
	}).fail(throwable -> {
		throwable.cause().printStackTrace();
	}).complete(promiseFlow -> {
		String hehe = promiseFlow.getParam("hehe");
		System.out.println("fin:" + hehe);
		promiseFlow.setParam("hehe", hehe + "->" + promiseFlow.toString());
	});

	public static void main(String[] args) {
		System.out.println("start");
		Map<String, Object> map = new HashMap<>();
		map.put("hehe", "flow1");
		f.start(map);

		map = new HashMap<>();
		map.put("hehe", "flow2");
		f.start(map);
		System.out.println("end");
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		System.out.println(uuid);
		try {
			System.out.println(Integer.parseInt(null));
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
		vertx.close();
	}




}

