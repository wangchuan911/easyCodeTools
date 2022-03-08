package my.hehe.demo.services.vo;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import my.hehe.demo.common.JdbcUtils;
import my.hehe.demo.common.annotation.ResZip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public class TextBaseVO extends ResourceVO implements ResZip {
	private String type;
	private String user;

	public String getType() {
		return type;
	}

	public TextBaseVO setType(String type) {
		this.type = type;
		return this;
	}

	public String getUser() {
		return user;
	}

	public TextBaseVO setUser(String user) {
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

	@Override
	public ResourceVO createRes(String text) {
		final String key = "TEXT:";
		int idx = -1;
		if ((text.toUpperCase().indexOf(key)) < 0) return null;
		idx = text.indexOf(":", key.length());
		String name = text.substring(key.length(), idx);
		text = text.substring(idx + 1);
		return new TextBaseVO().setType("TEXT").setResContent(text).setResName(name);
	}

	@Override
	public Future<Void> zipDataFile(ZipOutputStream zipOutputStream, Set<String> errorFile) {
		BufferedInputStream bis = null;
    /*AtomicInteger atomicInteger = new AtomicInteger(0);
    for (ResourceVO resourceVO : textBaseVOS) {
      if (resourceVO instanceof TextBaseVO) {
        TextBaseVO textBase = (TextBaseVO) resourceVO;
        atomicInteger.incrementAndGet();
        try {
          ResourceVO.writeZip(zipOutputStream, textBase.getResContent(),  textBase.getResName()+".txt");
        } catch (IOException e) {
          errorFile.add(textBase.toString() + " " + e.getMessage());

          e.printStackTrace();
          errorFile.add(textBase.toString() + " " + e.getMessage());
        }finally {
          if (atomicInteger.decrementAndGet() == 0) {
            handler.handle(null);
          }
        }
      }
    }*/
		return Future.future(promise -> {
			try {
				ResourceVO.writeZip(zipOutputStream, this.getResContent(), this.getResName() + ".txt");
				promise.complete();
			} catch (IOException e) {
				e.printStackTrace();
				errorFile.add(this.toString() + " " + e.getMessage());
				promise.fail(e);
			}
		});

	}
}



