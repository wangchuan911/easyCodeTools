package my.hehe.demo.services.vo;

import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.StreamUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarDeployVO extends DeployVO {
  JarOutputStream jarOutputStream = null;

  public synchronized void deploySingle(ZipInputStream zipInputStream, ZipEntry zipEntry) throws Throwable {
    try {
      String zipName = zipEntry.getName().substring(this.projectName.length() + 1);
      if (jarOutputStream == null) {

        String jarFile = this.getPath();
        File file = null;
        {
          file = new File(jarFile + ".jar");
          if (!file.exists()) {
            file = new File(jarFile + ".war");
          }
          if (!file.exists()) //throw new FileNotFoundException();
            file.createNewFile();
        }
        jarOutputStream = new JarOutputStream(new FileOutputStream(file));
      }
      jarOutputStream.putNextEntry(new JarEntry(zipName));
      StreamUtils.writeStream(zipInputStream, jarOutputStream);
    } finally {

    }
  }

  @Override
  public void deployAllAfter() {
    StreamUtils.close(jarOutputStream);
  }
}
