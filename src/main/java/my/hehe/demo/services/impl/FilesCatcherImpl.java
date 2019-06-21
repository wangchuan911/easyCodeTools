package my.hehe.demo.services.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import my.hehe.demo.services.FilesCatcher;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FilesCatcherImpl implements FilesCatcher {
  private static FilesCatcher filesCatcher = new FilesCatcherImpl();

  public static FilesCatcher getInstance() {
    return filesCatcher;
  }

  public static FilesCatcher getInstance(JsonObject option) {
    ((FilesCatcherImpl) filesCatcher).setConf(option);
    return filesCatcher;
  }

  JDBCClient jdbcClient = null;
  JsonObject confBuild = null;
  JsonObject confSourse = null;
  Set<String> pathsBuild = null;
  Set<String> pathsSourse = null;
  Map sourceToBuild = null;
  String tmpFilePath = null;

  private FilesCatcherImpl() {

  }

  public synchronized void setConf(JsonObject config) {


    if (confBuild == null && confSourse == null) {
      confBuild = new JsonObject();
      confSourse = new JsonObject();
    } else {
      return;
    }
    {
      JsonObject dbConfig = new JsonObject();
      String var1 = config.getJsonObject("data").getString("data3");
      String[] var2=var1.split("\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*");
      dbConfig.put("url",var2[0]);
      dbConfig.put("user",var2[1]);
      dbConfig.put("password",var2[2]);
      jdbcClient = JDBCClient.createNonShared(Vertx.vertx(), dbConfig);
    }

    config = config.getJsonObject("files");
    tmpFilePath = config.getString("tmpFilePath");
    JsonObject build = config.getJsonObject("build");
    Set<String> keys = build.getMap().keySet();
    keys.forEach(s -> {
      JsonArray objects = build.getJsonArray(s);
      objects.forEach(o -> {
        confBuild.put(o.toString(), s);
      });
    });
    pathsBuild = confBuild.getMap().keySet();

    JsonObject source = config.getJsonObject("source");
    keys = source.getMap().keySet();
    keys.forEach(s -> {
      JsonArray objects = source.getJsonArray(s);
      objects.forEach(o -> {
        confSourse.put(o.toString(), s);
      });
    });
    pathsSourse = confSourse.getMap().keySet();
    System.out.println(pathsSourse);
    System.out.println(pathsBuild);
    System.out.println(confBuild);
    System.out.println(confSourse);

    sourceToBuild = config.getJsonObject("sourceToBuild").getMap();

  }

  final static boolean isWindows = System.getProperty("os.name") != null && System.getProperty("os.name").indexOf("Windows") >= 0;

  final static Set<String> suffixSet = new HashSet<String>();
  final static FilenameFilter filenameFilter = (dir, name) -> {
    int idx = name.lastIndexOf(".");
    if (idx < 0) return true;
    String suffix = name.substring(idx + 1);
    if (suffixSet == null || suffixSet.size() == 0) {
      return true;
    } else {
      return suffixSet.contains(suffix);
    }
  };

  @Override
  public void dual(Set<String> fileList, Handler<AsyncResult<String>> outputBodyHandler) {
    Future future = Future.future();
    Set<String> successFile = null;
    Set<String> errorFile = null;
    try {
      future.setHandler(outputBodyHandler);
      if (fileList != null && fileList.size() > 0) {
        successFile = new ConcurrentHashSet<>();
        errorFile = new ConcurrentHashSet<>();
      }
      for (String fileName : fileList) {
        getFile(successFile, errorFile, fileName);
      }

      future.complete(zipFile(successFile, errorFile));
    } catch (Exception e) {
      e.printStackTrace();
      future.fail(e);
    }
  }

  private String fileNameCheck(String fileName) {
    if (pathsSourse.size() != 0 && sourceToBuild.size() != 0) {
      String prefix = null;
      for (Object object : sourceToBuild.keySet()) {
        String str = object.toString();
        if ((fileName.lastIndexOf(str) + str.length() == fileName.length())) {
          prefix = str;
        }
      }
      System.out.print(fileName);
      System.out.print("->");
      for (String source : pathsSourse) {
        if (fileName.indexOf(source) == 0) {

          fileName = fileName.replace(source, confSourse.getString(source));
        }
      }
      if (StringUtils.isNotEmpty(prefix))
        fileName = fileName.replace(prefix, sourceToBuild.get(prefix).toString());
      System.out.println(fileName);
    }
    return fileName;
  }

  private void getFile(Set<String> successFile, Set<String> errorFile, String fileName) {
    try {
      fileName = fileNameCheck(fileName);
      File rootFile = new File(fileName);
      if (rootFile.exists() && rootFile.canRead()) {
        if (rootFile.isFile()) {
          System.out.println(rootFile.getAbsolutePath());
          successFile.add(fileName);
        } else if (rootFile.isDirectory()) {
          String[] files = rootFile.list(filenameFilter);
          getFileSub(successFile, errorFile, rootFile, files);
        }
      } else {
        errorFile.add(fileName + (!rootFile.exists() ? " is not exists" : (!rootFile.canRead() ? " is unread" : "")));
      }
    } catch (Exception e) {
      e.printStackTrace();
      errorFile.add(fileName + " " + e.getMessage());
    } finally {
    }
  }

  private void getFileSub(Set<String> successFile, Set<String> errorFile, File rootFile, String[] files) {
    for (int i = 0; i < files.length; i++) {
      String subFileName = new StringBuilder(rootFile.getAbsolutePath()).append(File.separator).append(files[i]).toString();
      subFileName = fileNameCheck(subFileName);
      File subFile = new File(subFileName);
      if (!subFile.canRead()) {
        errorFile.add(subFileName);
        continue;
      }
      if (subFile.isFile()) {
        System.out.println(rootFile.getAbsolutePath());
        successFile.add(subFileName);
      } else if (subFile.isDirectory()) {
        getFile(successFile, errorFile, subFileName);
      } else {
        errorFile.add(subFileName);
      }
    }
  }

  private String zipFile(Set<String> successFile, Set<String> errorFile) {
    Calendar calendar = Calendar.getInstance();
    String zipOfFile = new StringBuilder(tmpFilePath).append(calendar.get(Calendar.YEAR)).append('_').append(calendar.get(Calendar.MONTH) + 1).append('_').append(calendar.get(Calendar.DATE)).append(".zip").toString();
    ZipOutputStream zipOutputStream = null;
    try {
      if (successFile != null || errorFile != null) {
        {
          File file = new File(zipOfFile);
          if (file.exists()) {
            file.delete();
          }
          file.createNewFile();
          zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        }
        BufferedInputStream bis = null;
        for (String file : successFile) {
          try {
            String zipFile = file;
            if (pathsBuild != null && pathsBuild.size() != 0) {
              for (String path : pathsBuild) {
                if (zipFile.indexOf(path) == 0) {
                  zipFile = zipFile.replace(path, confBuild.getString(path));
                  break;
                }
              }
            } else if (isWindows) {
              zipFile = zipFile.substring(zipFile.indexOf(':') + 2);
            }
            zipOutputStream.putNextEntry(new ZipEntry(zipFile));
            bis = new BufferedInputStream(new FileInputStream(new File(file)));
            /*int b;
            while ((b = bis.read()) != -1) {
              zipOutputStream.write(b); // 将字节流写入当前zip目录
            }*/
            this.writeZipStream(bis, zipOutputStream);
            System.out.println("create zip file :" + zipFile);
          } catch (Exception e) {
            e.printStackTrace();
            successFile.remove(file);
            if (errorFile == null) errorFile = new HashSet<>();
            errorFile.add(file + " copy fail:" + e.getMessage());
          } finally {
            close(bis);
          }
        }
      }
      return zipOfFile;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      BufferedInputStream bis2 = null;
      String err = this.createFailFile(errorFile);
      {
        File error = null;
        if (StringUtils.isNotEmpty(err) && (error = new File(err)).exists()) {
          try {
            zipOutputStream.putNextEntry(new ZipEntry("result.txt"));
            bis2 = new BufferedInputStream(new FileInputStream(error));
            writeZipStream(bis2, zipOutputStream);
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            error.delete();
            close(bis2);
          }
        }
      }

      close(zipOutputStream);
    }
  }

  private void writeZipStream(InputStream in, OutputStream out) throws IOException {
    int b;
    while ((b = in.read()) != -1) {
      out.write(b); // 将字节流写入当前zip目录
    }
  }

  private void close(InputStream in) {
    try {
      if (in != null) {
        in.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void close(OutputStream out) {
    try {
      if (out != null) {
        out.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String createFailFile(Set<String> fails) {
    Calendar calendar = Calendar.getInstance();
    String failOfFile = new StringBuilder(tmpFilePath).append(calendar.get(Calendar.YEAR)).append('_').append(calendar.get(Calendar.MONTH) + 1).append('_').append(calendar.get(Calendar.DATE)).append(".txt").toString();
    File file = new File(failOfFile);
    BufferedWriter stream = null;
    try {
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
      stream = new BufferedWriter(new FileWriter(file));
      for (String line : fails) {
        stream.write(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (stream != null) stream.close();
      } catch (Exception e) {
      }
    }
    return failOfFile;
  }
}
