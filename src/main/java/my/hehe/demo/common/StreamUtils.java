package my.hehe.demo.common;

import java.io.*;

public class StreamUtils {
  public static void close(InputStream in) {
    try {
      if (in != null) {
        in.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void close(OutputStream out) {
    try {
      if (out != null) {
        out.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void close(Reader in) {
    try {
      if (in != null) {
        in.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void close(Writer out) {
    try {
      if (out != null) {
        out.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void writeStream(InputStream in, OutputStream out) throws IOException {
    int b;
    while ((b = in.read()) != -1) {
      out.write(b); // 将字节流写入当前zip目录
    }
  }
}
