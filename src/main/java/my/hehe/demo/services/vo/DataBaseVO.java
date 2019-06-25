package my.hehe.demo.services.vo;

public class DataBaseVO extends ResouceVO {
  private String type;
  private String user;

  public String getType() {
    return type;
  }

  public DataBaseVO setType(String type) {
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
}
