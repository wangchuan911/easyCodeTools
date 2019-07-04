package my.hehe.demo.services.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.AsyncFlow;
import my.hehe.demo.common.StreamUtils;
import my.hehe.demo.common.annotation.ReflectionUtils;
import my.hehe.demo.services.FilesDeploy;
import my.hehe.demo.services.vo.DeployVO;
import my.hehe.demo.services.vo.ResourceVO;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

  public AtomicInteger onceUser = new AtomicInteger(0);

  Map<String, DeployVO> deployVOS = null;
  JsonObject deploys = null;

  private FilesDeployImpl() {

  }

  public synchronized void setConfig(JsonObject config) {


    if (deployVOS == null) {
      deployVOS = new HashMap<>();
    } else {
      return;
    }

    deploys = config.getJsonObject("deploy");

    Set<Class<? extends DeployVO>> subTypesOf = ReflectionUtils.getReflection().getSubTypesOf(DeployVO.class);

    {
      Set<String> keys = deploys.getMap().keySet();
      keys.forEach(key -> {
        JsonObject jsonObject = deploys.getJsonObject(key);
        String name = jsonObject.getString("mode");
        String modeToClass = name.replaceFirst(name.charAt(0) + "", (name.charAt(0) + "").toUpperCase()) + DeployVO.class.getSimpleName();
        System.out.println(String.format("regist Deploy class [ %s ]", modeToClass));
        for (Class aClass : subTypesOf) {
          if (modeToClass.equals(aClass.getSimpleName())) {
            try {
              Constructor constructor = aClass.getConstructor(null);
              DeployVO deployVO = (DeployVO) constructor.newInstance(null);
              deployVO.setPackageType(name);
              deployVO.setPath(jsonObject.getString("path"));
              deployVO.setProjectName(key);
              deployVO.setConfiguration(jsonObject);
              synchronized (deployVOS) {
                deployVOS.put(key, deployVO);
              }
            } catch (Throwable e) {
              e.printStackTrace();
            } finally {
              break;
            }
          }
        }
      });
    }

  }


  @Override
  public void dual(String zipFile, Handler<AsyncResult<String>> outputBodyHandler) {
    final StringBuilder error = new StringBuilder();

    Future future = Future.future();
    final String KEY_ZIP_FILE_STRAM = "zipInputStream";

    if (onceUser.get() > 0) {
      future.setHandler(outputBodyHandler);
      future.fail("人多");
      return;
    } else {
      future.setHandler((Handler<AsyncResult<String>>) asyncResult -> {
        onceUser.decrementAndGet();
        Future f = Future.future();
        f.setHandler(outputBodyHandler);
        if (asyncResult.succeeded()) {
          f.complete(asyncResult.result());
        } else {
          f.fail(asyncResult.cause());
        }
      });
    }
    onceUser.incrementAndGet();

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
        deployVOS.entrySet().forEach(stringDeployVOEntry -> {
          try {
            stringDeployVOEntry.getValue().deployAllBefore(zipInputStream);
          } catch (Throwable e) {
            error.append(e.toString()).append('\n');
          }
        });
        do {
          try {
            zipEntry = zipInputStream.getNextEntry();
            if (zipEntry == null) continue;
            String zipName = zipEntry.getName();
            String pj = null;
            int idx = zipName.indexOf(File.separator);
            if (idx > 0) {
              pj = zipName.substring(0, idx);
            }
            if (deployVOS.containsKey(pj)) {
              DeployVO deployVO = deployVOS.get(pj);
              deployVO.deploySingle(zipInputStream, zipEntry);
            }
          } catch (IOException e) {
            e.printStackTrace();
            error.append(e.toString()).append('\n');
            continue;
          } catch (Throwable e) {
            e.printStackTrace();
            error.append(e.toString()).append('\n');
            continue;
          }
        } while (zipEntry != null);
        deployVOS.entrySet().forEach(stringDeployVOEntry -> {
          try {
            stringDeployVOEntry.getValue().deployAllAfter(zipInputStream);
          } catch (Throwable e) {
            error.append(e.toString()).append('\n');
          }
        });
        asyncFlow.next();
      }).catchThen(asyncFlow -> {
        asyncFlow.getError().printStackTrace();
        future.fail(asyncFlow.getError());
      }).finalThen(flow -> {
        StreamUtils.close((ZipInputStream) flow.getParam().get(KEY_ZIP_FILE_STRAM));
        if (!flow.isError())
          future.complete(error.toString());
      }).start();
    } catch (Throwable e) {
      future.fail(e);
    }
  }
}
