package my.hehe.demo.common.annotation;

import io.vertx.core.json.JsonObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Verticle {
  boolean worker() default false;

  int instances() default 1;

  boolean multiThreaded() default false;
}
