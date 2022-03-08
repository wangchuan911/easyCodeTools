package my.hehe.demo.services.vo;

import my.hehe.demo.common.StreamUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarDeployVO extends ClassDeployVO {
	//  JarOutputStream jarOutputStream = null;
	File newFile = null;
	File file = null;
	//  Set<String> updateFile = new HashSet<>();
	Map<String, Set<String>> setMap = new HashMap<>();
	final boolean isWindows = (System.getProperty("os.name") != null && System.getProperty("os.name").indexOf("Windows") >= 0);
	final String CMD = isWindows ?
			"powershell.exe -Command \"cd '%s' ; & '%s" + File.separator + "bin" + File.separator + "jar.exe' -uf '%s' '%s'\""
			: "cd %s && %s" + File.separator + "bin" + File.separator + "jar -uf %s %s";

	Set<String> fileList = new HashSet<>();

	public synchronized void deploySingle(ZipInputStream zipInputStream, ZipEntry zipEntry) throws Throwable {
		super.deploySingle(zipInputStream, zipEntry);
    /*if (jarOutputStream == null) return;
    this.setRunning(true);
    try {
      String zipName = zipEntry.getName().substring(this.projectName.length() + 1);
      JarEntry jarEntry = null;
      jarOutputStream.putNextEntry(jarEntry = new JarEntry(zipName));
      StreamUtils.writeStream(zipInputStream, jarOutputStream);
      jarOutputStream.flush();
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

    }*/
		this.setRunning(true);
		if (file == null || zipEntry.getName().indexOf(this.getProjectName()) != 0) return;
    /*String command = String.format(CMD, this.getPath(), System.getProperty("jarBinPath"), newFile.getName(), zipEntry.getName().substring(this.getProjectName().length() + 1));
    System.out.println(command);
    Process p = Runtime.getRuntime().exec(command);
    p.waitFor();*/
		String dir = zipEntry.getName().substring(this.getProjectName().length() + 1).replace("\\", File.separator);
		/*dir = dir.substring(0, dir.indexOf(File.separator));*/
		System.out.println(dir);
		if (!fileList.contains(dir)) {
			fileList.add(dir);
		}
	}

	@Override
	public void deployAllAfter(ZipInputStream zipInputStream) throws Throwable {

		try {
			if (fileList.size() == 0) return;
			System.out.println();
			String command = String.format(CMD, this.getPath(), System.getProperty("jarBinPath"), newFile.getName(), fileList.stream().map(s -> !isWindows && s.indexOf("$") >= 0 ? s.replaceAll("\\$", "\\\\\\$") : s).collect(Collectors.joining(isWindows ? "' '" : " ")));
			System.out.println(command);
			if (isWindows) {
				Runtime.getRuntime().exec(command).waitFor();
			} else {
				Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command}).waitFor();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			throw e;
		} finally {
			fileList.clear();

			super.deployAllAfter(zipInputStream);
			if (!this.getRunning()) {
				if (newFile != null) newFile.delete();
				newFile = null;
				file = null;
				return;
			}

			if (newFile.exists()) {
				boolean isRename = false;
				String var = file.getAbsolutePath();
				isRename = file.renameTo(new File((this.getPath() + '-' + (Calendar.getInstance().getTimeInMillis()) + "-bak" + this.getPackageType())));
				System.out.println(isRename);
				isRename = newFile.renameTo(new File(var));
				System.out.println(isRename);
			}
			this.deleteFile(new File(this.getPath()));

			newFile = null;
			file = null;
			this.setRunning(false);
    /*if (!this.getRunning()) {
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
        ZipEntry zosEntry = new ZipEntry(e.getName());
        zosEntry.setComment(e.getComment());
        zosEntry.setExtra(e.getExtra());
        zosEntry.setLastModifiedTime(e.getLastModifiedTime());
        zosEntry.setTime(e.getTime());
        jarOutputStream.putNextEntry(zosEntry);
        if (!e.isDirectory()) {
          *//*int bytesRead;
          byte[] BUFFER = new byte[4096 * 1024];
          InputStream inputStream = jfile.getInputStream(e);
          while ((bytesRead = inputStream.read(BUFFER)) != -1) {
            jarOutputStream.write(BUFFER, 0, bytesRead);
          }*//*
          StreamUtils.writeStream(jfile.getInputStream(e), jarOutputStream);
          jarOutputStream.flush();
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
        if (newFile.exists()) {
          boolean isRename = false;
          String var = file.getAbsolutePath();
          isRename = file.renameTo(new File((this.getPath() + '-' + (Calendar.getInstance().getTimeInMillis()) + "-bak" + this.getPackageType())));
          System.out.println(isRename);
          isRename = newFile.renameTo(new File(var));
          System.out.println(isRename);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      updateFile.clear();
      newFile = null;
      file = null;
    }*/
		}
	}

	@Override
	public synchronized void deployAllBefore(ZipInputStream zipInputStream) throws Throwable {
		super.deployAllBefore(zipInputStream);
    /*try {
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

    }*/
		try {
			if (file == null) {
				String jarFilePath = this.getPath();
				String subfix;
				file = (file = new File(jarFilePath + (subfix = ".jar"))).exists() ? file : new File(jarFilePath + (subfix = ".war"));
				if (file.exists()) {
					newFile = new File(jarFilePath + File.separator + this.getProjectName() + subfix);
					if (!newFile.exists()) {
						this.mkdir(newFile.getParentFile());
						newFile.createNewFile();
					}
					StreamUtils.writeStreamAndClose(new FileInputStream(file), new FileOutputStream(newFile));
					this.setPackageType(subfix);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			file = null;
			newFile = null;
		} finally {

		}
	}

	private String reNameTmp(String fileName, String SubFix) {
		return fileName + '-' + (Calendar.getInstance().getTimeInMillis()) + "-tmp" + SubFix;
	}

	void deleteFile(File file) {
		if (!file.exists()) {
			return;
		} else if (file.isFile()) {
			file.delete();
		} else if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteFile(files[i]);
			}
			file.delete();
		}
	}

	void mkdir(File file) {
		if (file != null && !file.exists()) {
			file.mkdirs();
			this.mkdir(file.getParentFile());
		} else {
			return;
		}
	}
}
