package my.hehe.demo.services.vo;

public abstract class ResouceVO {
  String resName;
  String resContent;

  public String getResName() {
    return resName;
  }

  public ResouceVO setResName(String resName) {
    this.resName = resName;
    return this;
  }

  public String getResContent() {
    return resContent;
  }

  public ResouceVO setResContent(String resContent) {
    this.resContent = resContent;
    return this;
  }
}
