package my.hehe.demo.services.vo;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import my.hehe.demo.common.JdbcUtils;
import my.hehe.demo.common.annotation.ResTypeCheck;
import my.hehe.demo.common.annotation.ResZip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataBaseVO extends ResouceVO {
  private String type;
  private String user;

  public String getType() {
    return type;
  }

  public DataBaseVO setType(String type) {
    try {
      DataTypeToAlias.valueOf(type);
    } catch (IllegalArgumentException e) {
      type = DataAliasToType.valueOf(type).getValue();
    }
    this.type = type;
    return this;
  }

  public String getUser() {
    return user;
  }

  public DataBaseVO setUser(String user) {
    this.user = user;
    return this;
  }

  @Override
  public String toString() {
    return "{" +
      "type='" + type + '\'' +
      ", user='" + user + '\'' +
      ", resName='" + resName + '\'' +
      ", resContent='" + resContent + '\'' +
      '}';
  }

  @ResTypeCheck
  public static ResouceVO createRes(String text) {
    if (text.toUpperCase().indexOf("DATA:") < 0) return null;
    String[] var = text.split(":")[1].split("\\.");
    return new DataBaseVO().setUser(var[0].toUpperCase()).setType(var[1].toUpperCase()).setResName(var[2].toUpperCase());
  }

  @ResZip
  public static void zipDataFile(ZipOutputStream zipOutputStream, Set<ResouceVO> dataBaseVOS, Set<String> errorFile, Handler<Void> handler) throws Exception {
    BufferedInputStream bis = null;
    AtomicInteger atomicInteger = new AtomicInteger(0);
    for (ResouceVO resouceVO : dataBaseVOS) {
      if (resouceVO instanceof DataBaseVO) {
        DataBaseVO dataBaseVO = (DataBaseVO) resouceVO;
        atomicInteger.incrementAndGet();
        try {
          JdbcUtils.getJdbcClient("rimdbTest").querySingleWithParams("select sf_get_source_from_db(?,?,?) from dual", new JsonArray().add(dataBaseVO.getType()).add(dataBaseVO.getUser()).add(dataBaseVO.getResName()), jsonArrayAsyncResult -> {
            if (jsonArrayAsyncResult.succeeded()) {
              JsonArray jsonArray = null;
              String content = (jsonArray = jsonArrayAsyncResult.result()).getString(0);
              try {
                ResouceVO.writeZip(zipOutputStream, content, dataBaseVO.getUser() + "-" + dataBaseVO.getResName());
              } catch (IOException e) {
                errorFile.add(dataBaseVO.toString() + " " + e.getMessage());
              }
              if (atomicInteger.decrementAndGet() == 0) {
                handler.handle(null);
              }
            }
          });

        } catch (Exception e) {
          e.printStackTrace();
          errorFile.add(dataBaseVO.toString() + " " + e.getMessage());
        }
      }
    }
  }


}

enum DataTypeToAlias {

  PROCEDURE("PRD"), PACKAGE_BODY("PGB"), PACKAGE("PKG"), TYPE_BODY("TYB"), TRIGGER("TRI"), FUNCTION("FUN"), TYPE("TYE");

  private String value;

  DataTypeToAlias(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}

enum DataAliasToType {

  PRD("PROCEDURE"), PGB("PACKAGE_BODY"), PKG("PACKAGE"), TYB("TYPE_BODY"), TRI("TRIGGER"), FUN("FUNCTION"), TYE("TYPE");

  private String value;

  DataAliasToType(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

}

