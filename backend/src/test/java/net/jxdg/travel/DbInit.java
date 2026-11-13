package net.jxdg.travel;

import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 开发/部署辅助：初始化数据库（schema.sql + data.sql）。
 * 用法：mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=net.jxdg.travel.DbInit
 */
public class DbInit {
    public static void main(String[] args) throws Exception {
        String host = env("DB_HOST", "127.0.0.1");
        String port = env("MYSQL_PORT", "3306");
        String db = env("DB_NAME", "travel");
        String user = env("DB_USER", "travel");
        String password = env("DB_PASSWORD", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&allowMultiQueries=true";
        System.out.println("connecting " + url + " user=" + user);
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(true);
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(new java.io.PrintWriter(System.err));
            runner.setStopOnError(true);
            run(conn, runner, "db/schema.sql");
            run(conn, runner, "db/data.sql");
            System.out.println("database initialized");
        }
    }

    private static void run(Connection conn, ScriptRunner runner, String resource) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(
                DbInit.class.getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8))) {
            System.out.println("running " + resource);
            runner.runScript(reader);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }
}
