package ramble.db.h2;

import com.google.common.base.Preconditions;
import ramble.api.IdGenerator;

import java.net.UnknownHostException;
import java.sql.SQLException;

public class H2DbStoreSetup {

  public static void main(String args[]) throws UnknownHostException, SQLException {
    Preconditions.checkArgument(args.length == 2, "Must specify gossip-port and message-sync-port");
    int gossipPort = Integer.parseInt(args[0]);
    int messagePort = Integer.parseInt(args[1]);
    String id = IdGenerator.createId(gossipPort, messagePort);

    System.out.println("Creating H2 database tables for node-id " + id);
    H2DbStore.getOrCreateStore(IdGenerator.createId(gossipPort, messagePort)).runInitializeScripts();
  }
}
