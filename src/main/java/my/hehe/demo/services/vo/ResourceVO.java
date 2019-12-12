package my.hehe.demo.services.vo;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class ResourceVO {
  String resName;
  String resContent;

  public String getResName() {
    return resName;
  }

  public ResourceVO setResName(String resName) {
    this.resName = resName;
    return this;
  }

  public String getResContent() {
    return resContent;
  }

  public ResourceVO setResContent(String resContent) {
    this.resContent = resContent;
    return this;
  }

  public static synchronized void writeZip(ZipOutputStream zipOutputStream, String content, String fileName) throws IOException {
    zipOutputStream.putNextEntry(new ZipEntry(fileName));
    zipOutputStream.write(content.toString().getBytes());
    zipOutputStream.closeEntry();
  }

  public static File[] findRelaFile(File file) {
    File[] files = null;
    String fileName = file.getName();
    int idx = fileName.lastIndexOf(".");
    if (idx < 0) return null;
    switch (fileName.substring(idx)) {
      case ".class": {
        if (file.isFile()) {
          String realName = fileName.substring(0, idx);
          Pattern pattern = Pattern.compile(realName + "\\$.+");
          files = file.getParentFile().listFiles((dir, name) -> {
            boolean flag = name.endsWith(".class");
            flag = flag && pattern.matcher(name.substring(0, name.lastIndexOf(".class"))).matches();
            return flag;
          });
        }
      }
      break;
      default:
        break;
    }
    return files;
  }
}
