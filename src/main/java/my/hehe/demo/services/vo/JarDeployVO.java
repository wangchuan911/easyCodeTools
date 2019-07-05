package my.hehe.demo.services.vo;

import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.StreamUtils;

import java.io.*;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarDeployVO extends DeployVO {
  JarOutputStream jarOutputStream = null;
  File newFile = null;
  File file = null;
  Set<String> updateFile = new HashSet<>();

  public synchronized void deploySingle(ZipInputStream zipInputStream, ZipEntry zipEntry) throws Throwable {
    if (jarOutputStream == null) return;
    this.setRunning(true);
    try {
      String zipName = zipEntry.getName().substring(this.projectName.length() + 1);
      JarEntry jarEntry = null;
      jarOutputStream.putNextEntry(jarEntry = new JarEntry(zipName));
      StreamUtils.writeStream(zipInputStream, jarOutputStream);
      String var = jarEntry.getName().replace("\\", "/");
      if (var.equals(jarEntry.getName())) {
        updateFile.add(var);
        updateFile.add(jarEntry.getName().replace("/", "\\"));
      } else {
        updateFile.add(var);
        updateFile.add(jarEntry.getName());
      }
      System.out.println(var);
    } finally {

    }
  }

  @Override
  public void deployAllAfter(ZipInputStream zipInputStream) throws Throwable {
    if (!this.getRunning()) {
      try {
        StreamUtils.close(jarOutputStream);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        jarOutputStream = null;
      }
      if (newFile != null) newFile.delete();
      return;
    }
    JarFile jfile = null;
    try {
      if (!file.exists()) return;

      jfile = new JarFile(file);
      Enumeration<? extends JarEntry> entries = jfile.entries();
      System.out.println(jfile.getManifest());
      while (entries.hasMoreElements()) {
        ZipEntry e = entries.nextElement();
        if (updateFile.contains(e.getName())) {
          System.out.println(String.format("skip file :%s", e.getName()));
          continue;
        }
//        System.out.println("copy: " + e.getName());
        jarOutputStream.putNextEntry(e);
        if (!e.isDirectory()) {
          int bytesRead;
          byte[] BUFFER = new byte[4096 * 1024];
          InputStream inputStream = jfile.getInputStream(e);
          while ((bytesRead = inputStream.read(BUFFER)) != -1) {
            jarOutputStream.write(BUFFER, 0, bytesRead);
          }
        }
        jarOutputStream.closeEntry();
      }

    } catch (Throwable e) {
      e.printStackTrace();
      if (newFile != null) newFile.delete();
      throw e;
    } finally {
      try {
        jfile.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      try {
        StreamUtils.close(jarOutputStream);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        jarOutputStream = null;
      }
      try {
        boolean isRename = false;
        String var = file.getAbsolutePath();
        isRename = file.renameTo(new File((this.getPath() + '-' + (Calendar.getInstance().getTimeInMillis()) + "-bak" + this.getPackageType())));
        System.out.println(isRename);
        isRename = newFile.renameTo(new File(var));
        System.out.println(isRename);
      } catch (Exception e) {
        e.printStackTrace();
      }
      updateFile.clear();
      newFile = null;
      file = null;
    }
  }

  @Override
  public synchronized void deployAllBefore(ZipInputStream zipInputStream) throws Throwable {
    try {
      if (jarOutputStream == null) {
        String jarFile = this.getPath();
        {
          String subfix = ".jar";
          file = new File(jarFile + subfix);
          if (!file.exists()) {
            file = new File(jarFile + (subfix = ".war"));
          }
          if (file.exists()) {
            newFile = new File((jarFile + '-' + (Calendar.getInstance().getTimeInMillis()) + "-tmp" + subfix));
            newFile.createNewFile();
            this.setPackageType(subfix);
            jarOutputStream = new JarOutputStream(new FileOutputStream(newFile));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      jarOutputStream = null;
    } finally {

    }
  }
}
