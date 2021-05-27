package my.hehe.demo.common.flow;

import io.vertx.core.Handler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @Classname AbstractFlow
 * @Description TODO
 * @Author wang.zhidong
 * @Date 2021/5/26 10:24
 */
public abstract class AbstractFlow<T extends AbstractFlow> implements IFlow {

	public static class FlowUnit {
		int index;
		String name;
		IFlow.ResolveHandler resolve;
		IFlow.RejectAndRetryHandler reject;
		FlowUnit nextUnit;

		FlowUnit(int index, String name, IFlow.ResolveHandler resolve, IFlow.RejectAndRetryHandler reject) {
			this.name = name;
			this.resolve = resolve;
			this.reject = reject;
			this.index = index;
		}

		public String getName() {
			return name;
		}

		void handle(IFlowUnitState flowUnitState) throws Throwable {
			this.resolve.handle(flowUnitState);
		}
	}

	public static abstract class AbstractFlowUnitState implements IFlowUnitState {
		Map<String, Object> param;


		@Override
		public <V> V getParam(String key) {
			return (V) this.param.get(key);
		}

		@Override
		public void setParam(String key, Object value) {
			if (this.param == null)
				this.param = new HashMap<>();
			this.param.put(key, value);
		}
	}


	public abstract T then(ResolveHandler resolve, RejectAndRetryHandler reject);

	public abstract T then(ResolveHandler resolve);

	public abstract T then(String name, ResolveHandler resolve);

	public abstract T then(String name, ResolveHandler resolve, RejectAndRetryHandler reject);

	public abstract T allThen(String name, T... flows);

	public abstract T switchThen(String name, SwitchHandler stateFunction, T... flows);


	public abstract T fail(FailHandler fail);

	public abstract T complete(CompleteHandler complete);

	public abstract T success(SuccessHandler success);

	public abstract AbstractFlowStream start();

	public abstract AbstractFlowStream start(Map<String, Object> params);

	public abstract AbstractFlowStream start(Map<String, Object> params, Handler<IFlowEndUnitState> success, FailHandler fail);

	public abstract AbstractFlowStream start(Map<String, Object> params, Handler<IFlowEndUnitState> success, FailHandler fail, Handler<IFlowEndUnitState> complete);


	public abstract class AbstractFlowStream {
		FlowUnit head;
		STATE state = STATE.PREPARE;
		SuccessHandler success;
		CompleteHandler complete;
		FailHandler fail;

		public AbstractFlowStream(FlowUnit head, SuccessHandler success, FailHandler fail, CompleteHandler complete) {
			this.head = head;
			this.success = success;
			this.complete = complete;
			this.fail = fail;
		}

		abstract void execute(final FlowUnit flowUnit, final Map<String, Object> params);

		void end(STATE state, Map<String, Object> params) {
			this.isRuning(true);
			this.state = state;
			IFlowEndUnitState complete = getFlowEndUnitState().apply(params);
			switch (state) {
				case FINISH:
					if (this.success != null) {
						this.success.handle(complete);
					}
					break;
				case FAIL:
					complete.flowFail();
					break;
			}
			if (this.complete != null) {
				this.complete.handle(complete);
			}
		}

		abstract Function<Map<String, Object>, IFlowEndUnitState> getFlowEndUnitState();

		public void start() {
			this.start(null);
		}

		public void start(Map<String, Object> params) {
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
}
