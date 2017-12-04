package ramble.api;

import com.google.common.base.Joiner;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class IdGenerator {

  public static String createId(int gossipPort, int messageSyncPort) throws UnknownHostException {
    return Joiner.on("-").join(
            InetAddress.getLocalHost().getHostAddress(),
            gossipPort,
            messageSyncPort);
  }
}
