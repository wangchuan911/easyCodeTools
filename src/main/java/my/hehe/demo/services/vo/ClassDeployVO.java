package my.hehe.demo.services.vo;

import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ClassDeployVO extends DeployVO {

  public void setConfiguration(JsonObject jsonObject) {
  }

  @Override
  public void deploy(ZipInputStream zipInputStream, ZipEntry zipEntry) throws Throwable {
    FileOutputStream fileOutputStream = null;
    try {
      String zipName = zipEntry.getName();

      String deployName = this.getPath() + zipName.substring(this.projectName.length());
      File file = new File(deployName);
      if (file.exists()) {
        file.renameTo(new File(deployName + "." + ((Calendar.getInstance().getTimeInMillis()) + ".bak")));
      }
      File parentFile = file.getParentFile();
      if (!parentFile.exists()) {
        parentFile.mkdirs();
      }
      file.createNewFile();
      fileOutputStream = new FileOutputStream(file);
      StreamUtils.writeStream(zipInputStream, fileOutputStream);

    } finally {
      StreamUtils.close(fileOutputStream);
    }
  }
}
