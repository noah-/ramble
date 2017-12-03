package ramble.db.persistent;

import com.google.protobuf.ByteString;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ramble.api.RambleMessage;
import ramble.db.api.DbStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * TODO need to figure out what the correct primary key is for the messages
 */
public class PersistentDbStore implements DbStore {

  private static final Map<String, PersistentDbStore> CACHE = new HashMap<>();

  private final HikariDataSource hikariDataSource;

  private PersistentDbStore(String id) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
    hikariConfig.addDataSourceProperty("URL", "jdbc:h2:" + getDbFromId(id));
    hikariConfig.addDataSourceProperty("user", "sa");
    hikariConfig.addDataSourceProperty("password", "");
    this.hikariDataSource = new HikariDataSource(hikariConfig);
  }

  public static PersistentDbStore getOrCreateStore(String id) {
    return CACHE.computeIfAbsent(id, PersistentDbStore::new);
  }

  public void runInitializeScripts() throws SQLException {
    try (Connection con = hikariDataSource.getConnection(); Statement stmt = con.createStatement()) {
      if (stmt.execute("RUNSCRIPT FROM 'classpath:h2-init.sql'")) {
        stmt.getResultSet().close();
      }
    }
  }

  @Override
  public boolean exists(RambleMessage.SignedMessage message) {
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE sourceid = ? AND messagedigest = ? AND timestamp = ?)";

    try (Connection con = hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, message.getMessage().getSourceId());
      ps.setBytes(2,  message.getMessage().getMessageDigest().toByteArray());
      ps.setLong(3, message.getMessage().getTimestamp());

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
    String sql = "INSERT INTO messages(sourceid, message, messagedigest, timestamp, publickey, signature) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    try (Connection con = hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, message.getMessage().getSourceId());
      ps.setString(2, message.getMessage().getMessage());
      ps.setBytes(3, message.getMessage().getMessageDigest().toByteArray());
      ps.setLong(4, message.getMessage().getTimestamp());
      ps.setBytes(5, message.getPublicKey().toByteArray());
      ps.setBytes(6, message.getSignature().toByteArray());

      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO fix me
  // TODO: Convert database object to RambleMessage.SignedMessage, need to look at api.
  @Override
  public RambleMessage.SignedMessage get(String id) {
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE digest = ?)";
    try (Connection con = hikariDataSource.getConnection();
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

  // TODO fix me
  public HashSet<String> getRange(long start, long end) {
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE timestamp BETWEEN ? AND ?)";
    try (Connection con = hikariDataSource.getConnection();
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

  // TODO: make this more efficient
  @Override
  public Set<RambleMessage.SignedMessage> getAllMessages() {
    String sql = "SELECT sourceid, message, messagedigest, timestamp, publickey, signature FROM messages";
    Set<RambleMessage.SignedMessage> messages = new HashSet<>();

    try (Connection con = hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          RambleMessage.Message message = RambleMessage.Message.newBuilder()
                  .setSourceId(rs.getString(1))
                  .setMessage(rs.getString(2))
                  .setMessageDigest(ByteString.copyFrom(rs.getBytes(3)))
                  .setTimestamp(rs.getLong(4))
                  .build();

          messages.add(RambleMessage.SignedMessage.newBuilder()
                  .setMessage(message)
                  .setPublicKey(ByteString.copyFrom(rs.getBytes(5)))
                  .setSignature(ByteString.copyFrom(rs.getBytes(6)))
                  .build());
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return messages;
  }

  private String getDbFromId(String id) {
    return "~/rambleDB/" + id;
  }
}
