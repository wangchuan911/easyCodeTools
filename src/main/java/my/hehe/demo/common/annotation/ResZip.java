package my.hehe.demo.common.annotation;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import my.hehe.demo.services.vo.ResourceVO;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public interface ResZip {
	Future<Void> zipDataFile(ZipOutputStream zipOutputStream, Set<String> errorFile);
}
