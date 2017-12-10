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
import java.util.List;
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
    String sql = "SELECT EXISTS (SELECT * FROM MESSAGES WHERE SOURCEID = ? AND MESSAGEDIGEST = ? AND TIMESTAMP = ?)";

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
    String sql = "INSERT INTO MESSAGES(SOURCEID, MESSAGE, MESSAGEDIGEST, PARENTDIGEST, TIMESTAMP, PUBLICKEY, " +
            "SIGNATURE) VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection con = this.hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, message.getMessage().getSourceId());
      ps.setString(2, message.getMessage().getMessage());
      ps.setBytes(3, message.getMessage().getMessageDigest().toByteArray());
      // TODO: Implement parentDigest()
      // ps.setBytes(4, message.getMessage().getParentDigest.toByteArray());
      ps.setBytes(4, null);
      ps.setLong(5, message.getMessage().getTimestamp());
      ps.setBytes(6, message.getPublicKey().toByteArray());
      ps.setBytes(7, message.getSignature().toByteArray());

      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void store(RambleMessage.BulkSignedMessage messages) {
    if (!messages.getSignedMessageList().isEmpty()) {
      StringBuilder sql = new StringBuilder("INSERT INTO MESSAGES(SOURCEID, MESSAGE, MESSAGEDIGEST, PARENTDIGEST, " +
              "TIMESTAMP, PUBLICKEY, SIGNATURE) VALUES (?, ?, ?, ?, ?, ?, ?)");

      for (int i = 1; i < messages.getSignedMessageList().size(); i++) {
        sql.append(", (?, ?, ?, ?, ?, ?, ?)");
      }

      try (Connection con = this.hikariDataSource.getConnection();
           PreparedStatement ps = con.prepareStatement(sql.toString())) {

        List<RambleMessage.SignedMessage> messageList = messages.getSignedMessageList();
        for (int i = 0, j = 1; i < messageList.size(); i++, j+=6) {
          ps.setString(j, messageList.get(i).getMessage().getSourceId());
          ps.setString(j + 1, messageList.get(i).getMessage().getMessage());
          ps.setBytes(j + 2, messageList.get(i).getMessage().getMessageDigest().toByteArray());
          ps.setLong(j + 3, messageList.get(i).getMessage().getTimestamp());
          ps.setBytes(j + 4, messageList.get(i).getPublicKey().toByteArray());
          ps.setBytes(j + 5, messageList.get(i).getSignature().toByteArray());
        }

        ps.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Set<byte[]> getDigestRange(long startTimestamp, long endTimestamp) {
    String sql = "SELECT MESSAGEDIGEST FROM MESSAGES WHERE TIMESTAMP BETWEEN ? AND ?";
    try (Connection con = this.hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setLong(1, startTimestamp);
      ps.setLong(2, endTimestamp);
      Set<byte[]> digests = new HashSet<>();
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          digests.add(rs.getBytes(1));
        }
        return digests;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<RambleMessage.SignedMessage> getRange(long startTimestamp, long endTimestamp) {

    String sql = "SELECT SOURCEID, MESSAGE, MESSAGEDIGEST, PARENTDIGEST, TIMESTAMP, " +
            "PUBLICKEY, SIGNATURE FROM MESSAGES WHERE TIMESTAMP BETWEEN ? AND ?";

    try (Connection con = this.hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setLong(1, startTimestamp);
      ps.setLong(2, endTimestamp);
      try (ResultSet rs = ps.executeQuery()) {
        return resultSetToSignedMessages(rs);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This implementation isn't particularly efficient since it runs a SQL query to get the contents of an entire table,
   * a better way would be to use a SQL dump tool. For now this is probably sufficient since it shouldn't be called
   * often.
   */
  @Override
  public Set<RambleMessage.SignedMessage> getAllMessages() {
    return runSelectAllQuery("SELECT SOURCEID, MESSAGE, MESSAGEDIGEST, PARENTDIGEST, TIMESTAMP, " +
            "PUBLICKEY, SIGNATURE FROM MESSAGES");
  }

  @Override
  public Set<RambleMessage.SignedMessage> getMessages(Set<byte[]> messageDigest) {
    String sql = "SELECT SOURCEID, MESSAGE, MESSAGEDIGEST, PARENTDIGEST, TIMESTAMP, PUBLICKEY, SIGNATURE " +
            "FROM MESSAGES WHERE MESSAGEDIGEST IN (?)";

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

  public void updateBlockConfirmation(long ts) {
    String sql = "SELECT COUNT FROM BLOCKCONF WHERE TIMESTAMP = ?";
    try (Connection con = this.hikariDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setObject(1, ts);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int count = rs.getInt(1);
          // TODO: write back updated value or create new if doesn't exist
          // Look at how to make sure new entry is not made.
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void getFingerPrint() {

  }

  public void updateFingerPrint() {

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
              // TODO: implement setParentDigest
              //.setParentDigest(ByteString.copyFrom(rs.getBytes(4)))
              .setTimestamp(rs.getLong(5))
              .build();

      messages.add(RambleMessage.SignedMessage.newBuilder()
              .setMessage(message)
              .setPublicKey(ByteString.copyFrom(rs.getBytes(6)))
              .setSignature(ByteString.copyFrom(rs.getBytes(7)))
              .build());
    }
    return messages;
  }

  private String getDbFromId(String id) {
    return "~/rambleDB/" + id;
  }
}
