package my.hehe.demo.common.flow;

/**
 * @Classname IFlow
 * @Description TODO
 * @Author wang.zhidong
 * @Date 2021/5/26 10:20
 */
public interface IFlow {
	@FunctionalInterface
	interface RejectAndRetryHandler {
		void handle(IFlowRejectUnitState flowRejectUnitState) throws Throwable;
	}

	@FunctionalInterface
	interface ResolveHandler {
		void handle(IFlowUnitState flowUnitState) throws Throwable;
	}

	@FunctionalInterface
	interface SuccessHandler {
		void handle(IFlowEndUnitState flowUnitState);
	}

	@FunctionalInterface
	interface CompleteHandler extends SuccessHandler {

	}


	@FunctionalInterface
	interface FailHandler {
		void handle(IFlowFailEndUnitState flowUnitState);
	}

	@FunctionalInterface
	interface SwitchHandler {
		int apply(IFlowUnitState flowUnitState);
	}

	interface IFlowFailEndUnitState extends IFlowUnitState {
		Throwable cause();
	}

	interface IFlowRejectUnitState extends IFlowUnitState {
		Throwable cause();
	}

	interface IFlowUnitState {

		void next();

		void fail(String s);

		void fail(Throwable t);

		<T> T getParam(String key);

		void setParam(String key, Object value);
	}

	interface IFlowEndUnitState extends IFlowUnitState {
		boolean isFail();

		void flowFail();
	}
}
