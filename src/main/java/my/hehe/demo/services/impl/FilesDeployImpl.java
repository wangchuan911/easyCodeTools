package my.hehe.demo.services.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
    System.setProperty("jarBinPath", config.getString("jarBinPath"));

    Set<Class<? extends DeployVO>> subTypesOf = ReflectionUtils.getReflection().getSubTypesOf(DeployVO.class);

    {
      Set<String> keys = deploys.getMap().keySet();
      keys.stream().forEach(key -> {
        JsonObject jsonObject = deploys.getJsonObject(key);
        String name = jsonObject.getString("mode");
        String modeToClass = name.replaceFirst(name.charAt(0) + "", (name.charAt(0) + "").toUpperCase()) + DeployVO.class.getSimpleName();
        try {
          Constructor constructor = subTypesOf
            .stream()
            .filter(aClass -> modeToClass.equals(aClass.getSimpleName()))
            .findFirst().get()
            .getConstructor(null);
          DeployVO deployVO = (DeployVO) constructor.newInstance(null);
          deployVO.setPackageType(name);
          deployVO.setPath(jsonObject.getString("path"));
          deployVO.setProjectName(key);
          deployVO.setConfiguration(jsonObject);
          synchronized (deployVOS) {
            deployVOS.put(key, deployVO);
          }
          System.out.println(String.format("regist Deploy class [ %s ][ %s ]", modeToClass, deployVO.getProjectName()));
        } catch (Throwable e) {
          e.printStackTrace();
        }
        /*for (Class aClass : subTypesOf) {
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
        }*/
      });
    }

  }


  @Override
  public void dual(String zipFile, Handler<AsyncResult<String>> outputBodyHandler) {
    final StringBuilder error = new StringBuilder();
    final StringBuilder success = new StringBuilder("成功：\n");
    final Set<String> doing = new HashSet<>(deployVOS.size());
    Promise promise = Promise.promise();
    final String KEY_ZIP_FILE_STRAM = "zipInputStream";

    if (onceUser.get() > 0) {
      promise.future().setHandler(outputBodyHandler);
      promise.fail("人多");
      return;
    } else {
      promise.future().setHandler((Handler<AsyncResult<String>>) asyncResult -> {
        onceUser.decrementAndGet();
        Promise p = Promise.promise();
        p.future().setHandler(outputBodyHandler);
        if (asyncResult.succeeded()) {
          p.complete(asyncResult.result());
        } else {
          p.fail(asyncResult.cause());
        }
      });
    }
    onceUser.incrementAndGet();

    try {
      if (zipFile == null || zipFile.length() == 0) {
        promise.fail(new NullPointerException());
      }
      AsyncFlow.getInstance()
        .then("解析zip文件", flow -> {
          File zip = new File(zipFile);
          ZipInputStream zipInputStream = null;
          try {
            zipInputStream = new ZipInputStream(new FileInputStream(zip));
            flow.setParam(KEY_ZIP_FILE_STRAM, zipInputStream);
            flow.next();
          } catch (IOException e) {
            flow.fail("沒找到上传的压缩文件！");
          }
        }).then("写入前初始化", asyncFlow -> {
        ZipInputStream zipInputStream = asyncFlow.getParam(KEY_ZIP_FILE_STRAM, ZipInputStream.class);
        deployVOS.entrySet().stream().forEach(stringDeployVOEntry -> {
          try {
            stringDeployVOEntry.getValue().deployAllBefore(zipInputStream);
          } catch (Throwable e) {
            error.append(e.toString()).append('\n');
          }
        });
        asyncFlow.next();
      }).then("写入文件", asyncFlow -> {
        ZipInputStream zipInputStream = asyncFlow.getParam(KEY_ZIP_FILE_STRAM, ZipInputStream.class);
        ZipEntry zipEntry = null;
        do {
          try {
            zipEntry = zipInputStream.getNextEntry();
            if (zipEntry == null) continue;
            String zipName = zipEntry.getName();
            String pj = null;
            int idx = zipName.indexOf("\\");
            if (idx < 0) {
              idx = zipName.indexOf("/");
              if (idx < 0) continue;
            }
            if (idx > 0) {
              pj = zipName.substring(0, idx);
            }
            if (deployVOS.containsKey(pj)) {
              DeployVO deployVO = deployVOS.get(pj);
              deployVO.deploySingle(zipInputStream, zipEntry);
              doing.add(pj);
            }
          } catch (Throwable e) {
            e.printStackTrace();
            error.append(e.toString()).append('\n');
            continue;
          }
        } while (zipEntry != null);
        asyncFlow.next();
      }).catchThen(asyncFlow -> {
        asyncFlow.printStackTrace();
        promise.fail(asyncFlow);
      }).finalThen(flow -> {
        ZipInputStream zipInputStream = (ZipInputStream) flow.getParam().get(KEY_ZIP_FILE_STRAM);
        deployVOS.entrySet().stream().forEach(stringDeployVOEntry -> {
          try {
            stringDeployVOEntry.getValue().deployAllAfter(zipInputStream);
          } catch (Throwable e) {
            error.append(e.toString()).append('\n');
          }
        });
        for (String str : doing) {
          success.append(str).append('\n');
        }

        StreamUtils.close((ZipInputStream) flow.getParam().get(KEY_ZIP_FILE_STRAM));
        if (!flow.isError())
          promise.complete(success.toString() + '\n' + (error.length() == 0 ? "" : "错误：\n") + error.toString());
      }).start();
    } catch (Throwable e) {
      promise.fail(e);
    }
  }
}
