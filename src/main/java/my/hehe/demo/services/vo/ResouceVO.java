package my.hehe.demo.services.vo;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class ResouceVO {
  String resName;
  String resContent;

  public String getResName() {
    return resName;
  }

  public ResouceVO setResName(String resName) {
    this.resName = resName;
    return this;
  }

  public String getResContent() {
    return resContent;
  }

  public ResouceVO setResContent(String resContent) {
    this.resContent = resContent;
    return this;
  }

  public static synchronized void writeZip(ZipOutputStream zipOutputStream, String content, String fileName) throws IOException {
    zipOutputStream.putNextEntry(new ZipEntry(fileName));
    zipOutputStream.write(content.toString().getBytes());

  }
}
