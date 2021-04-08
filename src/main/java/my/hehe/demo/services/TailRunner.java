package my.hehe.demo.services;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TailRunner extends WebSocketRunner {
	private Set<TailExcutor> tailExcutors = new HashSet<>();
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
		/*check  state*/
		/*Set<TailExcutor> newExcutors = tailExcutors.stream().filter(excutor ->
				!(excutor.STATE == STATE.FINISH || excutor.STATE == STATE.FAIL || !excutor.isAlive())
		).collect(Collectors.toSet());
		tailExcutors.clear();
		tailExcutors.addAll(newExcutors);*/

		Optional<TailExcutor> optional = tailExcutors.stream().filter(tailExcutor ->
				target.equals(tailExcutor.id)
		).findFirst();

		TailExcutor tailExcutor;
		if (!optional.isPresent()) {
			tailExcutor = new TailExcutor(target);
			JsonObject config = this.webServiceConfig.getJsonObject(matcher.group(1)).getJsonObject(target);
			tailExcutor.encode = (config.getString("encode", Charset.defaultCharset().toString()));
			tailExcutor.path = (config.getString("path"));
			if (!new File(tailExcutor.path).exists()) return false;
			try {
				tailExcutor.webSocketSet.add(serverWebSocket);
				tailExcutor.start();
				tailExcutors.add(tailExcutor);
				System.out.println(String.format("create thread: %s", tailExcutor.toString()));
			} catch (Throwable e) {
				e.printStackTrace();
				tailExcutors.remove(tailExcutor);
				return false;
			}
		} else {
			tailExcutor = optional.get();
			System.out.println(String.format("add socket to thread: %s", tailExcutor.toString()));
			tailExcutor.webSocketSet.add(serverWebSocket);
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
		System.out.println("close");
		System.out.println(serverWebSocket);
		Matcher matcher = pattern.matcher(serverWebSocket.path());
		if (!matcher.find()) return;
		String target = matcher.group(2);
		System.out.println(this.tailExcutors);
		this.tailExcutors
				.stream()
				.filter(tailExcutor -> {
					System.out.println(target + "_" + tailExcutor.id);
					return target.equals(tailExcutor.id);
				})
				.collect(Collectors.toSet())
				.stream()
				.forEach(tailExcutor -> {
					/*System.out.println(tailExcutor.webSocketSet);
					System.out.println(tailExcutor.webSocketSet.contains(serverWebSocket));*/
					if (tailExcutor.webSocketSet.contains(serverWebSocket)) {
						tailExcutor.webSocketSet.remove(serverWebSocket);
						if (0 == tailExcutor.webSocketSet.size()) {
							tailExcutor.close();
						}
					}
				});
	}

	class TailExcutor extends Thread {
		final Set<ServerWebSocket> webSocketSet;
		final String id;
		String path;
		String encode;
		BufferedReader bufferedReader;
		Process process = null;

		public TailExcutor(String id) {
			this.id = id;
			this.webSocketSet = new HashSet<>();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TailExcutor excutor = (TailExcutor) o;
			return id.equals(excutor.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
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
					process.destroy();
				} catch (Throwable e1) {
				}
			}
			webSocketSet.clear();
		}

		public synchronized void close() {
			if (!this.isAlive() || !this.process.isAlive()) {
				return;
			}
			/*System.out.println("start release!");*/
			this.destroyAll();
			System.out.println(String.format("%s is ide ,release!", this.toString()));
			TailRunner.this.tailExcutors.remove(this);
		}

		private synchronized void openFailAndClose(Throwable e) {
			this.destroyAll();
			e.printStackTrace();
		}

	}

}

