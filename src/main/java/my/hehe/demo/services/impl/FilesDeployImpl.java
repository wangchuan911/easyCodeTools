package my.hehe.demo.services.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import my.hehe.demo.common.AsyncFlow;
import my.hehe.demo.common.PromiseFlow;
import my.hehe.demo.common.StreamUtils;
import my.hehe.demo.common.annotation.ReflectionUtils;
import my.hehe.demo.services.FilesDeploy;
import my.hehe.demo.services.vo.DeployVO;
import my.hehe.demo.services.vo.ResourceVO;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FilesDeployImpl implements FilesDeploy {

	final static boolean isWindows = System.getProperty("os.name") != null && System.getProperty("os.name").indexOf("Windows") >= 0;

	private static FilesDeployImpl filesDeploy;


	public synchronized static FilesDeployImpl getInstance(JsonObject option) {
		if (filesDeploy == null) {
			filesDeploy = new FilesDeployImpl();
			if (option != null) {
				filesDeploy.setConfig(option);
			}
		}
		return filesDeploy;
	}

	public AtomicInteger onceUser = new AtomicInteger(0);

	Map<String, DeployVO> deployVOS = new HashMap<>();
	JsonObject deploys = null;

	private FilesDeployImpl() {

	}

	final String KEY_ZIP_FILE_STRAM = "zipInputStream",
			KEY_ERROR_FILE_LIST = "error",
			KEY_SUCCESS_FILE_LIST = "doing";
	final PromiseFlow flow = new PromiseFlow("解析zip文件", flow -> {
		File zip = new File(flow.getParam("zipFile", String.class));
		ZipInputStream zipInputStream;
		try {
			zipInputStream = new ZipInputStream(new FileInputStream(zip));
			flow.setParam(KEY_ZIP_FILE_STRAM, zipInputStream);
			flow.next();
		} catch (IOException e) {
			flow.fail("沒找到上传的压缩文件！");
		}
	}).then("写入前初始化", asyncFlow -> {
		ZipInputStream zipInputStream = asyncFlow.getParam(KEY_ZIP_FILE_STRAM, ZipInputStream.class);
		List<String> error = asyncFlow.getParam(KEY_ERROR_FILE_LIST, List.class);
		deployVOS.entrySet().stream().forEach(stringDeployVOEntry -> {
			try {
				stringDeployVOEntry.getValue().deployAllBefore(zipInputStream);
			} catch (Throwable e) {
				error.add(e.toString());
			}
		});
		asyncFlow.next();
	}).then("写入文件", asyncFlow -> {
		ZipInputStream zipInputStream = asyncFlow.getParam(KEY_ZIP_FILE_STRAM, ZipInputStream.class);
		List<String> error = asyncFlow.getParam(KEY_ERROR_FILE_LIST, List.class);
		List<String> doing = asyncFlow.getParam(KEY_SUCCESS_FILE_LIST, List.class);
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
				error.add(e.toString());
				continue;
			}
		} while (zipEntry != null);
		asyncFlow.next();
	});

	void setConfig(JsonObject config) {
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
		final List<String> error = new LinkedList<>(),
				doing = new LinkedList();
		Promise promise = Promise.promise();
		promise.future().onComplete(outputBodyHandler);
		if (onceUser.get() > 0) {
			promise.fail("人多");
			return;
		}

		if (zipFile == null || zipFile.length() == 0) {
			promise.fail(new NullPointerException());
			return;
		}
		onceUser.incrementAndGet();
		Map<String, Object> params = new HashMap<>();
		params.put("zipFile", zipFile);
		params.put(KEY_ERROR_FILE_LIST, error);
		params.put(KEY_SUCCESS_FILE_LIST, doing);
		flow.start(params, flowEndUnitState -> {
		}, flowUnitState -> {
			flowUnitState.cause().printStackTrace();
			promise.fail(flowUnitState.cause());
		}, flowEndUnitState -> {
			onceUser.decrementAndGet();
			ZipInputStream zipInputStream = flowEndUnitState.getParam(KEY_ZIP_FILE_STRAM, ZipInputStream.class);
			deployVOS.entrySet().stream().forEach(stringDeployVOEntry -> {
				try {
					stringDeployVOEntry.getValue().deployAllAfter(zipInputStream);
				} catch (Throwable e) {
					error.add(e.toString());
				}
			});

			StreamUtils.close(zipInputStream);
			if (!flowEndUnitState.isFail()) {
				promise.complete(String.format("成功：%s\n失败：%s",
						doing.size() == 0 ? "无" : doing.stream().collect(Collectors.joining("\n")),
						error.size() == 0 ? "无" : error.stream().collect(Collectors.joining("\n"))
				));
			}
		});
	}
}
