package my.hehe.demo.services.vo;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import my.hehe.demo.common.JdbcUtils;
import my.hehe.demo.common.annotation.ResMatched;
import my.hehe.demo.common.annotation.ResZip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public class TableBaseVO extends ResourceVO implements ResZip {
	private String type;
	private String user;
	final static String key = "TABLE:";

	public String getType() {
		return type;
	}

	public TableBaseVO setType(String type) {
		try {
			TableTypeToAlias.valueOf(type);
		} catch (IllegalArgumentException e) {
			type = TableAliasToType.valueOf(type).getValue();
		}
		this.type = type;
		return this;
	}

	public String getUser() {
		return user;
	}

	public TableBaseVO setUser(String user) {
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

	@ResMatched(Matched.class)
	public TableBaseVO(String text) {
		String[] var = text.split(":")[1].split("@");
		this.setUser(var[1].toUpperCase()).setType(key.substring(0, key.length() - 1)).setResName(var[0].toUpperCase());
	}

	public static class Matched implements Function<String, Boolean> {
		@Override
		public Boolean apply(String s) {
			return !(s.toUpperCase().indexOf(key) < 0);
		}
	}

	@Override
	public Future<Void> zipDataFile(ZipOutputStream zipOutputStream, Set<String> errorFile) {
		BufferedInputStream bis = null;

		return Future.future(promise -> {
			try {
				StringBuilder text = new StringBuilder("ALTER ").append(this.getType()).append(' ').append(this.getResName()).append(" add (");
				StringBuilder body = new StringBuilder("SELECT a.column_name,A.DATA_TYPE || (CASE ")
						.append("          WHEN a.DATA_TYPE NOT IN ('DATE') THEN ")
						.append("           '(' || a.DATA_LENGTH || ')' ")
						.append("          ELSE ")
						.append("           '' ")
						.append("        END) ")
						.append("      ,a.data_default ")
						.append("      ,decode(a.NULLABLE, 'N', ' not null ', ' ') ")
						.append("  FROM user_tab_columns a ")
						.append(" WHERE a.table_name = ? ")
						.append("       AND NOT EXISTS (SELECT 1 ")
						.append("          FROM user_tab_columns@link_rimdb a1 ")
						.append("         WHERE a.column_name = a1.column_name ")
						.append("               AND a.table_name = a1.table_name)");
				JdbcUtils.getJdbcClient("rimdbTest").queryWithParams(body.toString(), new JsonArray().add(this.getResName()), jsonArrayAsyncResult -> {
					if (jsonArrayAsyncResult.succeeded()) {
						List<JsonArray> results = jsonArrayAsyncResult.result().getResults();
						for (int i = 0; i < results.size(); i++) {
							int j = 0;
							JsonArray JsonArray1ine = results.get(i);
							text.append(JsonArray1ine.getString(j)).append(' ');
							text.append(JsonArray1ine.getString(++j)).append(' ');
							if (JsonArray1ine.getString(++j) != null) {
								text.append("default ").append(JsonArray1ine.getString(j)).append(' ');
							}
							text.append(JsonArray1ine.getString(++j)).append((results.size() < i + 1) ? ',' : ')');
						}
						try {
							ResourceVO.writeZip(zipOutputStream, text.toString(), this.getUser() + "-" + this.getResName());
							promise.complete();
						} catch (IOException e) {
							errorFile.add(this.toString() + " " + e.getMessage());
							promise.fail(e);
						}
					}
				});

			} catch (Exception e) {
				e.printStackTrace();
				errorFile.add(this.toString() + " " + e.getMessage());
				promise.fail(e);
			}
		});
	}


}

enum TableTypeToAlias {

	TABLE("TAB");

	private String value;

	TableTypeToAlias(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}

enum TableAliasToType {

	TAB("TABLE");
	private String value;

	TableAliasToType(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}

