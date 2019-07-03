package my.hehe.demo.services.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.AsyncFlow;
import my.hehe.demo.common.StreamUtils;
import my.hehe.demo.common.annotation.ReflectionUtils;
import my.hehe.demo.common.annotation.ResTypeCheck;
import my.hehe.demo.common.annotation.ResZip;
import my.hehe.demo.services.FilesCatcher;
import my.hehe.demo.services.FilesDeploy;
import my.hehe.demo.services.vo.ResourceVO;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FilesDeployImpl implements FilesDeploy {

  final static boolean isWindows = System.getProperty("os.name") != null && System.getProperty("os.name").indexOf("Windows") >= 0;

  private static FilesDeployImpl filesDeploy = new FilesDeployImpl();

  public static FilesDeployImpl getInstance() {
    return filesDeploy;
  }

  public static FilesDeployImpl getInstance(JsonObject option) {
    ((FilesDeployImpl) filesDeploy).setConfig(option);
    return filesDeploy;
  }

  JsonObject confDeploy = null;
  JsonObject deploys = null;

  private FilesDeployImpl() {

  }

  public synchronized void setConfig(JsonObject config) {


    if (confDeploy == null) {
      confDeploy = new JsonObject();
    } else {
      return;
    }

    deploys = config.getJsonObject("deploy");


  }


  @Override
  public void dual(String zipFile, Handler<AsyncResult<String>> outputBodyHandler) {
    Future future = Future.future();
    final String KEY_ZIP_FILE_STRAM = "zipInputStream";
    future.setHandler(outputBodyHandler);
    try {
      if (zipFile == null || zipFile.length() == 0) {
        future.fail(new NullPointerException());
      }
      final Set<Class<? extends ResourceVO>> classSet = new HashSet<>();
      AsyncFlow.getInstance()
        .then("解析zip文件", flow -> {
          File zip = new File(zipFile);
          ZipInputStream zipInputStream = null;
          try {
            zipInputStream = new ZipInputStream(new FileInputStream(zip));
            flow.getParam().put(KEY_ZIP_FILE_STRAM, zipInputStream);
          } catch (IOException e) {
            flow.fail("沒找到上传的压缩文件！");
            return;
          }
          flow.next();
        }).then("写入文件", asyncFlow -> {
        ZipEntry zipEntry = null;
        ZipInputStream zipInputStream = (ZipInputStream) asyncFlow.getParam().get(KEY_ZIP_FILE_STRAM);
        FileOutputStream fileOutputStream = null;
        do {
          try {
            zipEntry = zipInputStream.getNextEntry();
            if (zipEntry == null) continue;
            String zipName = zipEntry.getName();
            String pj = null;
            System.out.println(zipName);
            int idx = -1;
            {
              idx = zipName.indexOf(File.separator);
              if (idx > 0) {
                pj = zipName.substring(0, idx);
              }
            }
            if (deploys.containsKey(pj)) {
              JsonObject deploy = deploys.getJsonObject(pj);
              String deployName = deploy.getString("path") + zipName.substring(idx);
              File file = new File(deployName);
              if (file.exists()) {
                file.delete();
              }
              File parentFile = file.getParentFile();
              if (!parentFile.exists()) {
                parentFile.mkdirs();
              }
              file.createNewFile();
              fileOutputStream = new FileOutputStream(file);
              StreamUtils.writeStream(zipInputStream, fileOutputStream);
            }
          } catch (IOException e) {
            e.printStackTrace();
            continue;
          } finally {
            StreamUtils.close(fileOutputStream);
          }
        } while (zipEntry != null);
        asyncFlow.next();
      }).catchThen(asyncFlow -> {
        asyncFlow.getError().printStackTrace();
        future.fail(asyncFlow.getError());
      }).finalThen(flow -> {
        StreamUtils.close((ZipInputStream) flow.getParam().get(KEY_ZIP_FILE_STRAM));
        if (!flow.isError())
          future.complete(null);
      }).start();
    } catch (Throwable e) {
      future.fail(e);
    }
  }
}
