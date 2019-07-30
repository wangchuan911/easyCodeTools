package my.hehe.demo.services.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.*;
import my.hehe.demo.common.annotation.ReflectionUtils;
import my.hehe.demo.common.annotation.ResTypeCheck;
import my.hehe.demo.common.annotation.ResZip;
import my.hehe.demo.services.FilesCatcher;
import my.hehe.demo.services.vo.ResourceVO;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FilesCatcherImpl implements FilesCatcher {
  private static FilesCatcher filesCatcher = new FilesCatcherImpl();

  public static FilesCatcher getInstance() {
    return filesCatcher;
  }

  public static FilesCatcher getInstance(JsonObject option) {
    ((FilesCatcherImpl) filesCatcher).setConfig(option);
    return filesCatcher;
  }

  JsonObject confBuild = null;
  JsonObject confSourse = null;
  Set<String> pathsBuild = null;
  Set<String> pathsSourse = null;
  Map sourceToBuild = null;
  String tmpFilePath = null;
  Set<Method> typeCheckMethod = null;
  Set<Method> typeZipMethod = null;

  private FilesCatcherImpl() {

  }

  public synchronized void setConfig(JsonObject config) {


    if (confBuild == null && confSourse == null) {
      confBuild = new JsonObject();
      confSourse = new JsonObject();
    } else {
      return;
    }

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

    Reflections reflections = ReflectionUtils.getReflection();
    typeCheckMethod = reflections.getMethodsAnnotatedWith(ResTypeCheck.class);
    typeZipMethod = reflections.getMethodsAnnotatedWith(ResZip.class);
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
    /*ZipOutputStream zipOutputStream = null;
      try {
      future.setHandler(outputBodyHandler);
      if (fileList == null || fileList.size() == 0) {
        future.fail(new NullPointerException());
      }
      final Set<String> simpleFiles = new ConcurrentHashSet<>();
      final Set<ResourceVO> unSimpleFiles = new ConcurrentHashSet<>();
      final Set<String> errorFile = new ConcurrentHashSet<>();
      //遍历文本，找文件
    fileList.forEach(fileName -> {
        int type = this.getTextMode(fileName);
        switch (type) {
          case 0:
            getFile(simpleFiles, errorFile, fileName);
            break;
          case 1:
            try {
              String[] var = fileName.split(":")[1].split("\\.");
              unSimpleFiles.add(new DataBaseVO().setUser(var[0].toUpperCase()).setType(var[1].toUpperCase()).setResName(var[2].toUpperCase()));
            } catch (NullPointerException e) {
              errorFile.add(fileName + " data is invail!");
            }
            break;
        }

      });
      //创建zip文件
      File zipOfFile = this.careateZipFile();
      zipOutputStream = new ZipOutputStream(new FileOutputStream(zipOfFile));
      //把一般文件压缩到zip文件中
      if (simpleFiles.size() > 0) {
        this.zipSimpleFile(zipOutputStream, simpleFiles, errorFile);
      }
      if (unSimpleFiles.size() > 0) {
        final ZipOutputStream zipOutputStream1 = zipOutputStream;
        this.zipDataFile(zipOutputStream, unSimpleFiles, errorFile, aVoid -> {
          //生成失败信息
          try {
            this.createFailFile(errorFile, zipOutputStream1);
          } catch (Exception e) {
            e.printStackTrace();
          }
          //关闭数据流
          this.close(zipOutputStream1);

          future.complete(zipOfFile.getAbsolutePath());
        });
      } else {
        //生成失败信息
        this.createFailFile(errorFile, zipOutputStream);

        //关闭数据流
        this.close(zipOutputStream);

        future.complete(zipOfFile.getAbsolutePath());
      }
    } catch (Exception e) {
      e.printStackTrace();
      //关闭数据流
      this.close(zipOutputStream);
      future.fail(e);
    }*/

    future.setHandler(outputBodyHandler);
    if (fileList == null || fileList.size() == 0) {
      future.fail(new NullPointerException());
    }
    final String KEY_ZIP_OS = "zipOutputStream";
    final String KEY_FIL_NAM = "zipOfFile";
    final Set<String> simpleFiles = new ConcurrentHashSet<>();
    final Set<ResourceVO> unSimpleFiles = new ConcurrentHashSet<>();
    final Set<String> errorFile = new ConcurrentHashSet<>();
    final Set<Class<? extends ResourceVO>> classSet = new HashSet<>();
    AsyncFlow.getInstance()
      .then("遍历文本，找文件", flow -> {
        try {
          fileList.forEach(fileName -> {
            if (typeCheckMethod == null) return;
            ResourceVO resourceVO = null;
            for (Method method : typeCheckMethod) {
              try {
                resourceVO = (ResourceVO) method.invoke(null, fileName);
                if (resourceVO != null) {
                  unSimpleFiles.add(resourceVO);
                  classSet.add(resourceVO.getClass());
                  break;
                }
              } catch (Throwable e) {
                e.printStackTrace();
                errorFile.add(fileName + " " + e.getMessage());
              }
            }
            if (resourceVO == null) {
              getFile(simpleFiles, errorFile, fileName);
            }
          });
        } catch (Exception e) {
          flow.fail(e);
        }
        flow.next();
      }).then("创建zip文件", flow -> {

      try {
        File zipOfFile = this.careateZipFile();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipOfFile));
        flow.getParam().put(KEY_FIL_NAM, zipOfFile);
        flow.getParam().put(KEY_ZIP_OS, zipOutputStream);
      } catch (Exception e) {
        flow.fail(e);
      }
      flow.next();
    }).then("把一般文件压缩到zip文件中", flow -> {
      try {
        if (simpleFiles.size() > 0) {
          ZipOutputStream zipOutputStream = (ZipOutputStream) flow.getParam().get(KEY_ZIP_OS);
          this.zipSimpleFile(zipOutputStream, simpleFiles, errorFile);
        }
      } catch (Exception e) {
        flow.fail(e);
      }
      flow.next();
    }).then("把特殊文件压缩到zip文件中", flow -> {

      try {
        if (unSimpleFiles.size() > 0 && typeZipMethod != null) {
          ZipOutputStream zipOutputStream = (ZipOutputStream) flow.getParam().get(KEY_ZIP_OS);
          AtomicInteger atomicInteger = new AtomicInteger(classSet.size());
          for (Method method : typeZipMethod) {
            method.invoke(null, zipOutputStream, unSimpleFiles, errorFile, (Handler<Void>) aVoid -> {
              if (atomicInteger.decrementAndGet() == 0) {
                flow.next();
              }
            });
          }
        } else {
          flow.next();
        }
      } catch (Exception e) {
        flow.fail(e);
      }
    }).catchThen(asyncFlow -> {
      asyncFlow.getError().printStackTrace();
      future.fail(asyncFlow.getError());
    }).finalThen(flow -> {
      ZipOutputStream zipOutputStream = (ZipOutputStream) flow.getParam().get(KEY_ZIP_OS);
      File zipOfFile = (File) flow.getParam().get(KEY_FIL_NAM);
      //生成失败信息
      try {

        this.createFailFile(errorFile, zipOutputStream);
      } catch (Exception e) {
        e.printStackTrace();
      }
      //关闭数据流
      this.close(zipOutputStream);
      if (!flow.isError())
        future.complete(zipOfFile.getAbsolutePath());
    }).start();
  }

  private File careateZipFile() {
    File file = null;
    try {
      String zipOfFile = null;
      Calendar calendar = Calendar.getInstance();
      zipOfFile = new StringBuilder(tmpFilePath).append(calendar.getTimeInMillis()).append(".zip").toString();
      file = new File(zipOfFile);
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return file;
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

  private void getFile(Set<String> simpleFiles, Set<String> errorFile, String fileName) {
    try {
      fileName = fileNameCheck(fileName);
      File rootFile = new File(fileName);
      if (rootFile.exists() && rootFile.canRead()) {
        if (rootFile.isFile()) {
          System.out.println(rootFile.getAbsolutePath());
          simpleFiles.add(fileName);
          File[] files = ResourceVO.findRelaFile(rootFile);
          for (int i = 0; i < (files==null?0:files.length); i++) {
            simpleFiles.add(files[i].getAbsolutePath());
          }
        } else if (rootFile.isDirectory()) {
          String[] files = rootFile.list(filenameFilter);
          getFileSub(simpleFiles, errorFile, rootFile, files);
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

  private void getFileSub(Set<String> simpleFiles, Set<String> errorFile, File rootFile, String[] files) {
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
        simpleFiles.add(subFileName);
        File[] subfiles = ResourceVO.findRelaFile(subFile);
        for (int i1 = 0; i1 < (subfiles==null?0:subfiles.length); i1++) {
          simpleFiles.add(subfiles[i1].getAbsolutePath());
        }
      } else if (subFile.isDirectory()) {
        getFile(simpleFiles, errorFile, subFileName);
      } else {
        errorFile.add(subFileName);
      }
    }
  }

  private void zipSimpleFile(ZipOutputStream zipOutputStream, Set<String> simpleFiles, Set<String> errorFile) {
    try {
      if (simpleFiles != null || errorFile != null) {

        for (String file : simpleFiles) {
          try {
            zipProjectFile(file, zipOutputStream);
          } catch (Exception e) {
            e.printStackTrace();
            simpleFiles.remove(file);
            if (errorFile == null) errorFile = new HashSet<>();
            errorFile.add(file + " copy fail:" + e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void zipProjectFile(String file, ZipOutputStream zipOutputStream) throws Exception {
    BufferedInputStream bis = null;
    try {
      String zipFile = file;
      boolean pathIsChange = false;
      if (pathsBuild != null && pathsBuild.size() != 0) {
        for (String path : pathsBuild) {
          if (zipFile.indexOf(path) == 0) {
            zipFile = zipFile.replace(path, confBuild.getString(path));
            pathIsChange = true;
            break;
          }
        }
      }
      if (!pathIsChange && isWindows) {
        zipFile = zipFile.substring(zipFile.indexOf(':') + 1);
      }
      zipOutputStream.putNextEntry(new ZipEntry(zipFile));
      bis = new BufferedInputStream(new FileInputStream(new File(file)));
      this.writeZipStream(bis, zipOutputStream);
      System.out.println("create zip file :" + zipFile);
      zipOutputStream.closeEntry();
    } finally {
      close(bis);
    }
  }


  private void writeZipStream(InputStream in, OutputStream out) throws IOException {
    /*int b;
    while ((b = in.read()) != -1) {
      out.write(b); // 将字节流写入当前zip目录
    }*/
    StreamUtils.writeStream(in, out);
  }

  private void close(InputStream in) {
    /*try {
      if (in != null) {
        in.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }*/
    StreamUtils.close(in);
  }

  private void close(OutputStream out) {
    /*try {
      if (out != null) {
        out.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }*/
    StreamUtils.close(out);
  }

  private void createFailFile(Set<String> fails, ZipOutputStream zipOutputStream) throws Exception {
    if (fails.size() == 0) return;
    zipOutputStream.putNextEntry(new ZipEntry("result.txt"));
    for (String str : fails) {
      str += "\r\n";
      zipOutputStream.write(str.getBytes());
    }
    zipOutputStream.closeEntry();
  }


}
