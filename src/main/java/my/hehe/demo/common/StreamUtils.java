package my.hehe.demo.common;

import java.io.*;

public class StreamUtils {
  public static void close(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
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
