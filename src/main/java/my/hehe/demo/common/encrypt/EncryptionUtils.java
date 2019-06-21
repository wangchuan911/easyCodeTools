package my.hehe.demo.common.encrypt;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EncryptionUtils {
  static Charset CHARSET = StandardCharsets.UTF_8;
  static Base64 base64 = new Base64();
  ;

  public static String encrypt(String text, String encodingAesKey) throws AesException {
    byte[] aesKey = Base64.decodeBase64(encodingAesKey + "=");
    ByteGroup byteCollector = new ByteGroup();
    byte[] textBytes = text.getBytes(CHARSET);
    byte[] networkBytesOrder = getNetworkBytesOrder(textBytes.length);

    // randomStr + networkBytesOrder + text + appid
    byteCollector.addBytes(networkBytesOrder);
    byteCollector.addBytes(textBytes);

    // ... + pad: 使用自定义的填充方式对明文进行补位填充
    byte[] padBytes = PKCS7Encoder.encode(byteCollector.size());
    byteCollector.addBytes(padBytes);

    // 获得最终的字节流, 未加密
    byte[] unencrypted = byteCollector.toBytes();

    try {
      // 设置加密模式为AES的CBC模式
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
      IvParameterSpec iv = new IvParameterSpec(aesKey, 0, 16);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

      // 加密
      byte[] encrypted = cipher.doFinal(unencrypted);

      // 使用BASE64对加密后的字符串进行编码
      String base64Encrypted = base64.encodeToString(encrypted);

      return base64Encrypted;
    } catch (Exception e) {
      e.printStackTrace();
      throw new AesException(AesException.EncryptAESError);
    }
  }

  static byte[] getNetworkBytesOrder(int sourceNumber) {
    byte[] orderBytes = new byte[4];
    orderBytes[3] = (byte) (sourceNumber & 0xFF);
    orderBytes[2] = (byte) (sourceNumber >> 8 & 0xFF);
    orderBytes[1] = (byte) (sourceNumber >> 16 & 0xFF);
    orderBytes[0] = (byte) (sourceNumber >> 24 & 0xFF);
    return orderBytes;
  }

  public static String decrypt(String text, String encodingAesKey) throws AesException {
    byte[] aesKey = Base64.decodeBase64(encodingAesKey + "=");
    byte[] original;
    try {
      // 设置解密模式为AES的CBC模式
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      SecretKeySpec key_spec = new SecretKeySpec(aesKey, "AES");
      IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
      cipher.init(Cipher.DECRYPT_MODE, key_spec, iv);

      // 使用BASE64对密文进行解码
      byte[] encrypted = Base64.decodeBase64(text);

      // 解密
      original = cipher.doFinal(encrypted);
    } catch (Exception e) {
      e.printStackTrace();
      throw new AesException(AesException.DecryptAESError);
    }
    String xmlContent, from_appid;
    try {
      // 去除补位字符
      byte[] bytes = PKCS7Encoder.decode(original);

      // 分离16位随机字符串,网络字节序和AppId
      byte[] networkOrder = Arrays.copyOfRange(bytes, 0, 4);

      int xmlLength = recoverNetworkBytesOrder(networkOrder);

      xmlContent = new String(Arrays.copyOfRange(bytes, 4, 4 + xmlLength), CHARSET);
    } catch (Exception e) {
      e.printStackTrace();
      throw new AesException(AesException.IllegalBuffer);
    }
    return xmlContent;

  }

  // 还原4个字节的网络字节序
  public static int recoverNetworkBytesOrder(byte[] orderBytes) {
    int sourceNumber = 0;
    for (int i = 0; i < 4; i++) {
      sourceNumber <<= 8;
      sourceNumber |= orderBytes[i] & 0xff;
    }
    return sourceNumber;
  }

  public static void main(String[] args) throws Exception {


  }
}
