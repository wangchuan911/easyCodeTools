package my.hehe.demo.services;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TailRunner extends WebSocketRunner {
	private Map<String, Executor> executorMap = new HashMap<>();
	static Pattern pattern = Pattern.compile("(?:\\/[\\w\\d]+)*\\/(tail)\\/([\\w\\d]+)", Pattern.CASE_INSENSITIVE);
	JsonObject webServiceConfig;

	public TailRunner(JsonObject config) {
		this.webServiceConfig = config;
	}

	@Override
	public synchronized boolean open(ServerWebSocket serverWebSocket) {
		Matcher matcher = pattern.matcher(serverWebSocket.path());
		if (!matcher.find()) return false;
		String target = matcher.group(2);
		if (StringUtils.isEmpty(target)) return false;
		/*check  state*/
		/*Set<TailExcutor> newExcutors = tailExcutors.stream().filter(excutor ->
				!(excutor.STATE == STATE.FINISH || excutor.STATE == STATE.FAIL || !excutor.isAlive())
		).collect(Collectors.toSet());
		tailExcutors.clear();
		tailExcutors.addAll(newExcutors);*/

		Executor executor = executorMap.get(target);
		if (executor == null || !executor.isAlive() || executor.isInterrupted()) {
			Executor deadExecutor = (executor != null) ? executor : null;
			JsonObject config = this.webServiceConfig.getJsonObject(matcher.group(1)).getJsonObject(target);
			executor = new Executor(target,
					config.getString("path"),
					config.getString("encode", Charset.defaultCharset().toString()));
			if (!new File(executor.path).exists()) return false;
			try {
				executor.webSocketSet.add(serverWebSocket);
				executor.start();
				executorMap.put(target, executor);
				System.out.println(String.format("create thread: %s", executor.toString()));
			} catch (Throwable e) {
				e.printStackTrace();
				executorMap.remove(target);
				return false;
			}
			if (deadExecutor != null) {
				if (deadExecutor.webSocketSet.size() > 0)
					executor.webSocketSet.addAll(deadExecutor.webSocketSet);
			}

		} else {
			System.out.println(String.format("add socket to thread: %s", executor.toString()));
			executor.webSocketSet.add(serverWebSocket);
		}
		serverWebSocket.closeHandler(aVoid -> {
			this.close(serverWebSocket);
		});
		/*System.out.println(this.tailExcutors);
		System.out.println("open");
		System.out.println(serverWebSocket);*/
		return true;
	}

	@Override
	public synchronized void close(ServerWebSocket serverWebSocket) {
		/*System.out.println("close");
		System.out.println(serverWebSocket);*/
		Matcher matcher = pattern.matcher(serverWebSocket.path());
		if (!matcher.find()) return;
		String target = matcher.group(2);
		/*System.out.println(this.tailExcutors);*/

		Executor executor = this.executorMap.get(target);
		if (executor != null) {
			if (executor.webSocketSet.contains(serverWebSocket)) {
				executor.webSocketSet.remove(serverWebSocket);
				if (0 == executor.webSocketSet.size()) {
					executor.close();
				}
			}
		}
	}

	class Executor extends Thread {
		final String target;
		final Set<ServerWebSocket> webSocketSet;
		final String path;
		String encode;
		BufferedReader bufferedReader;
		Process process = null;

		public Executor(String target, String path, String encode) {
			this.target = target;
			this.path = path;
			this.encode = encode;
			this.webSocketSet = new HashSet<>();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Executor excutor = (Executor) o;
			return getId() == excutor.getId();
		}

		@Override
		public int hashCode() {
			return Objects.hash(getId());
		}

		@Override
		public void run() {
			this.init();
			String line;
			try {
				while ((line = bufferedReader.readLine()) != null) {
					if (webSocketSet.size() == 0) {
						return;
					}
					for (ServerWebSocket socket : webSocketSet) {
						try {
							socket.writeTextMessage(line);
						} catch (Throwable e) {
							synchronized (webSocketSet) {
								webSocketSet.remove(socket);
								socket.close();
							}
						}
					}
				}
			} catch (Throwable e) {
				this.openFailAndClose(e);
			} finally {
				this.close();
			}
		}

		private void init() {
			try {
				String command = String.format(" tail -f %s", this.path);
//      command = "ping -t localhost";
				System.out.println(String.format("run command [%s]", command));
				process = Runtime.getRuntime().exec(command);
				bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), this.encode));
			} catch (Throwable e) {
				this.openFailAndClose(e);
			}
		}

		private void destroyAll() {
			if (process != null) {
				try {
					process.destroyForcibly();
				} catch (Throwable e1) {
				}
			}
			if (!this.isInterrupted()) {
				this.interrupt();
			}
			webSocketSet.clear();
		}

		public synchronized void close() {
			if (this.process.isAlive() || !this.isInterrupted()) {
				this.destroyAll();
				System.out.println(String.format("%s process is killing !", this.toString()));
			}
			TailRunner.this.executorMap.remove(Executor.this.target);
			/*System.out.println("start release!");*/
		}

		private synchronized void openFailAndClose(Throwable e) {
			this.close();
			e.printStackTrace();
		}

	}

}

