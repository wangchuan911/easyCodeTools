package my.hehe.demo.services.vo;

import my.hehe.demo.common.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassDeployVO extends DeployVO {

  private String backUpDir;
  private SimpleDateFormat simpleDateFormat= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

  @Override
  public void deployAllAfter(ZipInputStream zipInputStream) throws Throwable {
    this.backUpDir =null;
    super.deployAllAfter(zipInputStream);
  }

  @Override
  public void deployAllBefore(ZipInputStream zipInputStream) throws Throwable {
    this.backUpDir = null;
    this.backUpDir =(".backUp"+File.separator+simpleDateFormat.format(new Date())).replace("\\", "/");;
    super.deployAllBefore(zipInputStream);
  }

  @Override
  public void deploySingle(ZipInputStream zipInputStream, ZipEntry zipEntry) throws Throwable {
    FileOutputStream fileOutputStream = null;
    try {
      String zipName = zipEntry.getName();
      String deployName = this.getPath() + zipName.substring(this.projectName.length()).replace("\\", "/");
      File file = new File(deployName);
      System.out.println(zipName + "-->" + deployName);
      if (file.exists()) {
        String bakName = null;
        /*bakName = deployName + "." + ((Calendar.getInstance().getTimeInMillis()) + ".bak");
        bakName = bakName.replace("\\", "/");*/
        bakName = new StringBuilder(deployName).insert(this.getPath().length(),File.separator+backUpDir).toString().replace("\\", "/");
        File bakFile=new File(bakName);
        File bakPraFile=bakFile.getParentFile();
        if(!bakPraFile.exists()){
          bakPraFile.mkdirs();
        }
        System.out.println(String.format("%s exists ! backup to %s %s", deployName, bakName, file.renameTo(bakFile) ? "success" : "fail"));

      }
      File parentFile = file.getParentFile();
      if (!parentFile.exists()) {
        parentFile.mkdirs();
      }
      file = new File(deployName);
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
      fileOutputStream = new FileOutputStream(file);
      StreamUtils.writeStream(zipInputStream, fileOutputStream);

    } finally {
      StreamUtils.close(fileOutputStream);
    }
  }
}
