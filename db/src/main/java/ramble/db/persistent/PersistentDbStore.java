package ramble.db.persistent;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ramble.api.RambleMessage;
import ramble.db.api.DbStore;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


/**
 * TODO need to figure out what the correct primary key is for the messages
 */
public class PersistentDbStore implements DbStore {

  private static final HikariConfig HIKARI_CONFIG = new HikariConfig();
  private static final HikariDataSource HIKARI_DS;

  static {
    HIKARI_CONFIG.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
    HIKARI_CONFIG.addDataSourceProperty("URL", "jdbc:h2:~/rambleDB");
    HIKARI_CONFIG.addDataSourceProperty("user", "sa");
    HIKARI_CONFIG.addDataSourceProperty("password", "");
    HIKARI_DS = new HikariDataSource(HIKARI_CONFIG);
  }

  public static void runInitializeScripts() throws SQLException {
    try (Connection con = HIKARI_DS.getConnection(); Statement stmt = con.createStatement()) {
      if (stmt.execute("RUNSCRIPT FROM 'classpath:h2-init.sql'")) {
        stmt.getResultSet().close();
      }
    }
  }

  @Override
  public boolean exists(RambleMessage.SignedMessage message) {
    String digest = message.getMessage().getMessageDigest().toString(StandardCharsets.UTF_8);
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE digest = ?)";

    try (Connection con = HIKARI_DS.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, digest);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next() && rs.getBoolean(1)) {
          return true;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  @Override
  public void store(RambleMessage.SignedMessage message) {
    String sql = "INSERT INTO messages(digest, publickey, timestamp, msg) VALUES (?, ?, ?, ?)";
    long ts = message.getMessage().getTimestamp();
    try (Connection con = HIKARI_DS.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, message.getMessage().getMessageDigest().toStringUtf8());
      ps.setString(2, message.getPublicKey().toStringUtf8());
      ps.setLong(3, ts);
      ps.setString(4, message.getMessage().getMessage());

      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: Convert database object to RambleMessage.SignedMessage, need to look at api.
  @Override
  public RambleMessage.SignedMessage get(String id) {
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE digest = ?)";
    try (Connection con = HIKARI_DS.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          // TODO: Convert database object to RambleMessage.SignedMessage, need to look at api.
          return null;
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public HashSet<String> getRange(long start, long end) {
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE timestamp BETWEEN ? AND ?)";
    try (Connection con = HIKARI_DS.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      HashSet<String> result = new HashSet<String>();
      ps.setLong(1, start);
      ps.setLong(2, end);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(rs.getString(1));
        }

        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
