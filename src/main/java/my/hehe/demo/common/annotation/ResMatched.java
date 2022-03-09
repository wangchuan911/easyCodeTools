package my.hehe.demo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * @Classname Res
 * @Description TODO
 * @Author wang.zhidong
 * @Date 2022/3/9 09:18
 */

@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResMatched {
	Class<? extends Function<String, Boolean>> value();
}
