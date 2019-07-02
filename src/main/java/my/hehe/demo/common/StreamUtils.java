package my.hehe.demo.common;

import java.io.InputStream;
import java.io.OutputStream;

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
}
