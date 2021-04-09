package my.hehe.demo.services.impl;

import io.vertx.core.*;
import io.vertx.core.impl.future.CompositeFutureImpl;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FilesCatcherImpl implements FilesCatcher {
	private static FilesCatcherImpl filesCatcher;

	public static FilesCatcher getInstance(JsonObject option) {
		if (filesCatcher == null) {
			filesCatcher = new FilesCatcherImpl();
			if (option != null) {
				filesCatcher.setConfig(option);
			}
		}
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

	final String KEY_ZIP_OS = "zipOutputStream";
	final String KEY_FIL_NAM = "zipOfFile";

	synchronized void setConfig(JsonObject config) {
		confBuild = new JsonObject();
		confSourse = new JsonObject();
		tmpFilePath = config.getString("tmpFilePath");
		JsonObject build = config.getJsonObject("build");
		Set<String> keys = build.getMap().keySet();
		keys.stream().forEach(s -> {
			JsonArray objects = build.getJsonArray(s);
			objects.stream().forEach(o -> {
				if (confBuild.containsKey(o.toString())) {
					Object obj = confBuild.getValue(o.toString());
					if (obj instanceof JsonArray) {
						confBuild.put(o.toString(), ((JsonArray) obj).add(s));
					} else {
						confBuild.put(o.toString(), new JsonArray().add(obj).add(s));
					}
				} else {
					confBuild.put(o.toString(), s);
				}
			});
		});
		pathsBuild = confBuild.getMap().keySet();

		JsonObject source = config.getJsonObject("source");
		keys = source.getMap().keySet();
		keys.stream().forEach(s -> {
			JsonArray objects = source.getJsonArray(s);
			objects.stream().forEach(o -> {
				if (!confSourse.containsKey(o.toString())) {
					confSourse.put(o.toString(), s);
				} else {
					Object obj = confSourse.getValue(o.toString());
					if (obj instanceof JsonArray) {
						confSourse.put(o.toString(), ((JsonArray) obj).add(s));
					} else {
						confSourse.put(o.toString(), new JsonArray().add(obj).add(s));
					}
				}

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
	PromiseFlow promiseFlow =
			new PromiseFlow("遍历文本，找文件", flow -> {
				Set<String> fileList = flow.getParam("fileList", Set.class),
						errorFile = flow.getParam("errorFile", Set.class),
						simpleFiles = flow.getParam("simpleFiles", Set.class);
				Set<ResourceVO> unSimpleFiles = flow.getParam("unSimpleFiles", Set.class);
				Set<Class<? extends ResourceVO>> classSet = flow.getParam("classSet", Set.class);
				fileList.stream().forEach(fileName -> {
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
				flow.next();
			}).then("创建zip文件", flow -> {
				try {
					File zipOfFile = this.careateZipFile();
					ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipOfFile));
					flow.setParam(KEY_FIL_NAM, zipOfFile);
					flow.setParam(KEY_ZIP_OS, zipOutputStream);
				} catch (Exception e) {
					flow.fail(e);
				}
				flow.next();
			}).then("把一般文件压缩到zip文件中", flow -> {
				Set<String> simpleFiles = flow.getParam("simpleFiles", Set.class),
						errorFile = flow.getParam("errorFile", Set.class);
				if (simpleFiles.size() > 0) {
					ZipOutputStream zipOutputStream = flow.getParam(KEY_ZIP_OS, ZipOutputStream.class);
					this.zipSimpleFile(zipOutputStream, simpleFiles, errorFile);
				}
				flow.next();
			}).then("把特殊文件压缩到zip文件中", flow -> {
				Set<ResourceVO> unSimpleFiles = flow.getParam("unSimpleFiles", Set.class);
				Set<String> errorFile = flow.getParam("errorFile", Set.class);
				if (unSimpleFiles.size() > 0 && typeZipMethod.size() > 0) {
					ZipOutputStream zipOutputStream = flow.getParam(KEY_ZIP_OS, ZipOutputStream.class);
					CompositeFutureImpl
							.all(typeZipMethod
									.stream()
									.map(method ->
											Future.future(promise1 -> {
												try {
													method.invoke(null, zipOutputStream, unSimpleFiles, errorFile, (Handler<Void>) aVoid -> {
														promise1.complete();
													});
												} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
													promise1.fail(e);
												}
											}))
									.toArray(Future[]::new))
							.onSuccess(compositeFuture -> {
								flow.next();
							})
							.onFailure(throwable -> {
								flow.fail(throwable);
							});
				} else {
					flow.next();
				}
			}).catchThen(asyncFlow -> {
				asyncFlow.printStackTrace();
			}).finalThen(flow -> {

			});

	@Override
	public void dual(Set<String> fileList, Handler<AsyncResult<String>> outputBodyHandler) {
		Promise promise = Promise.promise();
		promise.future().onComplete(outputBodyHandler);
		if (fileList == null || fileList.size() == 0) {
			promise.fail(new NullPointerException());
		}
		/*final Set<String> simpleFiles = new HashSet<>();
		final Set<ResourceVO> unSimpleFiles = new HashSet<>();
		final Set<String> errorFile = new HashSet<>();
		final Set<Class<? extends ResourceVO>> classSet = new HashSet<>();*/

		Map<String, Object> map = new HashMap<>();
		map.put("simpleFiles", new HashSet<String>());
		map.put("unSimpleFiles", new HashSet<ResourceVO>());
		map.put("errorFile", new HashSet<String>());
		map.put("fileList", fileList);
		map.put("classSet", new HashSet<Class<? extends ResourceVO>>());
		promiseFlow.start(map, throwable -> {
			promise.fail(throwable);
		}, flow -> {
			ZipOutputStream zipOutputStream = flow.getParam(KEY_ZIP_OS, ZipOutputStream.class);
			File zipOfFile = flow.getParam(KEY_FIL_NAM, File.class);
			//生成失败信息
			try {
				this.createFailFile(flow.getParam("errorFile", Set.class), zipOutputStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//关闭数据流
			StreamUtils.close(zipOutputStream);
			promise.complete(zipOfFile.getAbsolutePath());
		});
//		AsyncFlow.getInstance()
//				.then("遍历文本，找文件", flow -> {
//					fileList.stream().forEach(fileName -> {
//						if (typeCheckMethod == null) return;
//						ResourceVO resourceVO = null;
//						for (Method method : typeCheckMethod) {
//							try {
//								resourceVO = (ResourceVO) method.invoke(null, fileName);
//								if (resourceVO != null) {
//									unSimpleFiles.add(resourceVO);
//									classSet.add(resourceVO.getClass());
//									break;
//								}
//							} catch (Throwable e) {
//								e.printStackTrace();
//								errorFile.add(fileName + " " + e.getMessage());
//							}
//						}
//						if (resourceVO == null) {
//							getFile(simpleFiles, errorFile, fileName);
//						}
//					});
//					flow.next();
//				}).then("创建zip文件", flow -> {
//			try {
//				File zipOfFile = this.careateZipFile();
//				ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipOfFile));
//				flow.setParam(KEY_FIL_NAM, zipOfFile);
//				flow.setParam(KEY_ZIP_OS, zipOutputStream);
//			} catch (Exception e) {
//				flow.fail(e);
//			}
//			flow.next();
//		}).then("把一般文件压缩到zip文件中", flow -> {
//			if (simpleFiles.size() > 0) {
//				ZipOutputStream zipOutputStream = flow.getParam(KEY_ZIP_OS, ZipOutputStream.class);
//				this.zipSimpleFile(zipOutputStream, simpleFiles, errorFile);
//			}
//			flow.next();
//		}).then("把特殊文件压缩到zip文件中", flow -> {
//			/*if (unSimpleFiles.size() > 0 && typeZipMethod != null) {
//				ZipOutputStream zipOutputStream = flow.getParam(KEY_ZIP_OS, ZipOutputStream.class);
//				AtomicInteger atomicInteger = new AtomicInteger(classSet.size());
//				for (Method method : typeZipMethod) {
//					try {
//						method.invoke(null, zipOutputStream, unSimpleFiles, errorFile, (Handler<Void>) aVoid -> {
//							if (atomicInteger.decrementAndGet() == 0) {
//								flow.next();
//							}
//						});
//					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//						flow.fail(e);
//						return;
//					}
//				}
//			} else {
//				flow.next();
//			}*/
//			if (unSimpleFiles.size() > 0 && typeZipMethod.size() > 0) {
//				ZipOutputStream zipOutputStream = flow.getParam(KEY_ZIP_OS, ZipOutputStream.class);
//				CompositeFuture
//						.all(typeZipMethod
//								.stream()
//								.map(method ->
//										Future.future(promise1 -> {
//											try {
//												method.invoke(null, zipOutputStream, unSimpleFiles, errorFile, (Handler<Void>) aVoid -> {
//													promise1.complete();
//												});
//											} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//												promise1.fail(e);
//											}
//										}))
//								.collect(Collectors.toList()))
//						.onSuccess(compositeFuture -> {
//							flow.next();
//						})
//						.onFailure(throwable -> {
//							flow.fail(throwable);
//						});
//			} else {
//				flow.next();
//			}
//		}).catchThen(asyncFlow -> {
//			asyncFlow.printStackTrace();
//			promise.fail(asyncFlow);
//		}).finalThen(flow -> {
//			ZipOutputStream zipOutputStream = (ZipOutputStream) flow.getParam().get(KEY_ZIP_OS);
//			File zipOfFile = (File) flow.getParam().get(KEY_FIL_NAM);
//			//生成失败信息
//			try {
//				this.createFailFile(errorFile, zipOutputStream);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			//关闭数据流
//			this.close(zipOutputStream);
//			if (!flow.isError())
//				promise.complete(zipOfFile.getAbsolutePath());
//		}).start();
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
			final String tmpFileName = StringUtils.isNotEmpty(prefix)
					? fileName.replace(prefix, sourceToBuild.get(prefix).toString())
					: fileName;
			for (String source : pathsSourse) {
				if (fileName.indexOf(source) == 0) {
					Object value = null;
					if ((value = confSourse.getValue(source)) instanceof JsonArray) {
						JsonArray jsonArray = (JsonArray) value;
						fileName = jsonArray.stream().map(o ->
								new File(tmpFileName.replace(source, o.toString()))
						).filter(file -> file.exists())
								.max((o1, o2) -> (o1.lastModified() > o2.lastModified() ? 1 : -1))
								.get()
								.getAbsolutePath();
						break;
					} else {
						fileName = tmpFileName.replace(source, confSourse.getString(source));
						break;
					}
				}
			}
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
//          System.out.println(rootFile.getAbsolutePath());
					simpleFiles.add(fileName);
					File[] files = ResourceVO.findRelaFile(rootFile);
					for (int i = 0; i < (files == null ? 0 : files.length); i++) {
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
//        System.out.println(rootFile.getAbsolutePath());
				simpleFiles.add(subFileName);
				File[] subfiles = ResourceVO.findRelaFile(subFile);
				for (int i1 = 0; i1 < (subfiles == null ? 0 : subfiles.length); i1++) {
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
		if (simpleFiles != null || errorFile != null) {
			simpleFiles.stream().forEach(file -> {
				try {
					zipProjectFile(file, zipOutputStream);
				} catch (Exception e) {
					e.printStackTrace();
					simpleFiles.remove(file);
					errorFile.add(file + " copy fail:" + e.getMessage());
				}
			});
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

	private void createFailFile(Set<String> fails, ZipOutputStream zipOutputStream) throws IOException {
		if (fails.size() == 0) return;
		zipOutputStream.putNextEntry(new ZipEntry("result.txt"));
		for (String str : fails) {
			str += "\r\n";
			zipOutputStream.write(str.getBytes());
		}
		zipOutputStream.closeEntry();
	}


}
