package my.hehe.demo.services.vo;

import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class DeployVO {
  String projectName = null;
  String path = null;
  String packageType = null;
  volatile boolean running = false;

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPackageType() {
    return packageType;
  }

  public void setPackageType(String packageType) {
    this.packageType = packageType;
  }

  public abstract void deploySingle(ZipInputStream zipInputStream, ZipEntry zipEntry) throws Throwable;

  public void setConfiguration(JsonObject jsonObject) {

  }

  public void deployAllAfter(ZipInputStream zipInputStream) throws Throwable {

  }

  public void deployAllBefore(ZipInputStream zipInputStream) throws Throwable {

  }

  public synchronized boolean getRunning() {
    return running;
  }

  public synchronized void setRunning(boolean running) {
    this.running = running;
  }
}
