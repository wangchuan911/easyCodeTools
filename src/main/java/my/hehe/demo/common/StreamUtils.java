package my.hehe.demo.common;

import java.io.*;
import java.nio.file.Files;

public class StreamUtils {

  private static final int BUFFER_SIZE = 8192;

  public static void close(Closeable closeable) {
    try {
      if (closeable != null) {
        if (closeable instanceof Flushable) {
          ((Flushable) closeable).flush();
        }
        closeable.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void writeStreamAndClose(InputStream in, OutputStream out) throws IOException {
    writeStream(in, out);
    close(in);
    close(out);
  }

  public static void writeStream(InputStream in, OutputStream out) throws IOException {
    /*int b;
    while ((b = in.read()) != -1) {
      out.write(b); // 将字节流写入当前zip目录
    }*/
    long nread = 0L;
    byte[] buf = new byte[BUFFER_SIZE];
    int n;
    while ((n = in.read(buf)) > 0) {
      out.write(buf, 0, n);
      nread += n;
    }
  }
}
