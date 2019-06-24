package my.hehe.demo.services.vo;

public abstract class ResouceVO {
  String resName;
  String resContent;

  public String getResName() {
    return resName;
  }

  public void setResName(String resName) {
    this.resName = resName;
  }

  public String getResContent() {
    return resContent;
  }

  public void setResContent(String resContent) {
    this.resContent = resContent;
  }
}
