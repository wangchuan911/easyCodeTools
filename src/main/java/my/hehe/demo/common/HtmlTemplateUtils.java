package my.hehe.demo.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import my.hehe.demo.common.annotation.UtilsInital;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * @Classname HtmlTamplateUtils
 * @Description TODO
 * @Author wang.zhidong
 * @Date 2021/4/4 22:49
 */
public class HtmlTemplateUtils {

	public static void goHtml(TemplateEngine engine,JsonObject jsonObject, RoutingContext routingContext, String page) {
		engine.render(jsonObject, String.format("templates/%s.html", page), res -> {
			if (res.succeeded()) {
				routingContext.response().putHeader("Content-Type", "text/html").end(res.result());
			} else {
				res.cause().printStackTrace();
				routingContext.fail(res.cause());
			}
		});
	}

	public static TemplateEngine createTemplateEngine(Vertx vertx) {
		ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(vertx);
		{
			// 定时模板解析器,表示从类加载路径下找模板
			ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
      /*// 设置模板的前缀，我们设置的是templates目录
      templateResolver.setPrefix("templates");
      // 设置后缀为.html文件
      templateResolver.setSuffix(".html");*/
			templateResolver.setTemplateMode("HTML5");
			templateResolver.setCharacterEncoding("utf-8");
			org.thymeleaf.TemplateEngine templateEngine = engine.unwrap();
			templateEngine.setTemplateResolver(templateResolver);
		}
		return engine;
	}
}
