package ramble.db.persistent;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import ramble.api.RambleMessage;
import ramble.db.api.DbStore;

public class PersistentDbStore implements DbStore {
    private static Connection connection = null;

    @Override
    public boolean exists(RambleMessage.SignedMessage message) {
        if (connection == null)
            return false;

        String digest = message.getMessage().getMessageDigest().toString(StandardCharsets.UTF_8);

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * from messages WHERE digest = \'" + digest + "\'");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void store(RambleMessage.SignedMessage message) {
        if (connection == null)
            return;

        try {
            Statement s = connection.createStatement();

            String digest = message.getMessage().getMessageDigest().toString(StandardCharsets.UTF_8);
            String key = message.getPublicKey().toStringUtf8();
            long ts = message.getMessage().getTimestamp();
            String msg = message.getMessage().getMessage();
            String sql = "INSERT INTO messages VALUES (\'" + digest + "\' , \'" + key + "\' , " + ts + " , \'" + msg + "\' )";

            s.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*  TODO: Implement this */
    @Override
    public RambleMessage.SignedMessage get(String id) {
        if (connection == null) {
            throw new UnsupportedOperationException();
        }

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * from messages WHERE digest = \'" + id + "\'");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                throw new UnsupportedOperationException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        throw new UnsupportedOperationException();
    }

    public PersistentDbStore() {
        if (connection != null) {
            return;
        }

        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection("jdbc:h2:~rambleDB", "test", "test");
            Statement s = connection.createStatement();
            s.execute("create table if not exists messages (digest varchar(255), publickey varchar(255), timestamp int, msg varchar(255))");
            s.execute("create table if not exists iplist (publickey varchar(255), ipaddress int, timestamp int, count int)");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}