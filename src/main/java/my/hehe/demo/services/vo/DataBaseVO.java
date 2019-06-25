package my.hehe.demo.services.vo;

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

  public DataBaseVO relaeseDataBase() {
    return null;
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

