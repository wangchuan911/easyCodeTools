package my.hehe.demo.common.annotation;

import my.hehe.demo.ApplicationVerticle;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class ReflectionUtils {
  static Reflections reflections = null;

  static {
    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
    configurationBuilder.setUrls(ClasspathHelper.forPackage(ApplicationVerticle.class.getPackage().getName()));
    configurationBuilder.addScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner());
    reflections = new Reflections(configurationBuilder);
  }

  public static Reflections getReflection() {
    return reflections;
  }
}
