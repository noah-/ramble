package ramble.db.h2;

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
 * Stores data in H2
 *
 * @see <a href="http://www.h2database.com/html/main.html">H2 Database</a>
 */
public class H2DbStore implements DbStore {

  private static final Map<String, H2DbStore> CACHE = new HashMap<>();

  private final HikariDataSource hikariDataSource;

  private H2DbStore(String id) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
    hikariConfig.addDataSourceProperty("URL", "jdbc:h2:" + getDbFromId(id));
    hikariConfig.addDataSourceProperty("user", "sa");
    hikariConfig.addDataSourceProperty("password", "");
    this.hikariDataSource = new HikariDataSource(hikariConfig);
  }

  public static H2DbStore getOrCreateStore(String id) {
    return CACHE.computeIfAbsent(id, H2DbStore::new);
  }

  public void runInitializeScripts() throws SQLException {
    try (Connection con = this.hikariDataSource.getConnection(); Statement stmt = con.createStatement()) {
      if (stmt.execute("RUNSCRIPT FROM 'classpath:h2-init.sql'")) {
        stmt.getResultSet().close();
      }
    }
  }

  @Override
  public boolean exists(RambleMessage.SignedMessage message) {
    String sql = "SELECT EXISTS (SELECT * FROM messages WHERE sourceid = ? AND messagedigest = ? AND timestamp = ?)";

    try (Connection con = this.hikariDataSource.getConnection();
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

    try (Connection con = this.hikariDataSource.getConnection();
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

  @Override
  public Set<RambleMessage.SignedMessage> getRange(long startTimestamp, long endTimestamp) {
    return runSelectAllQuery("SELECT sourceid, message, messagedigest, timestamp, publickey, signature FROM " +
            "messages WHERE timestamp BETWEEN ? AND ?)");
  }

  /**
   * This implementation isn't particularly efficient since it runs a SQL query to get the contents of an entire table,
   * a better way would be to use a SQL dump tool. For now this is probably sufficient since it shouldn't be called
   * often.
   */
  @Override
  public Set<RambleMessage.SignedMessage> getAllMessages() {
    return runSelectAllQuery("SELECT sourceid, message, messagedigest, timestamp, publickey, signature " +
            "FROM messages");
  }

  @Override
  public Set<RambleMessage.SignedMessage> getMessages(Set<byte[]> messageDigest) {
    String sql = "SELECT sourceid, message, messagedigest, timestamp, publickey, signature FROM messages WHERE " +
            "messagedigest IN (?)";

    try (Connection con = this.hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setObject(1, messageDigest.toArray());
      try (ResultSet rs = ps.executeQuery()) {
        return resultSetToSignedMessages(rs);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<RambleMessage.SignedMessage> runSelectAllQuery(String sql) {
    try (Connection con = this.hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        return resultSetToSignedMessages(rs);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<RambleMessage.SignedMessage> resultSetToSignedMessages(ResultSet rs) throws SQLException {
    Set<RambleMessage.SignedMessage> messages = new HashSet<>();

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
    return messages;
  }

  private String getDbFromId(String id) {
    return "~/rambleDB/" + id;
  }
}
