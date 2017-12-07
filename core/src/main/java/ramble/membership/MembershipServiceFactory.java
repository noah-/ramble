package ramble.membership;

import ramble.api.MembershipService;
import ramble.membership.gossip.ApacheGossipMembershipService;

import java.io.IOException;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;


/**
 * Factory for {@link MembershipService}. Currently, there is only one implementation of {@link MembershipService}, so this
 * class is very simple. However, having this class around makes it a lot easier to add a new implementation of
 * {@link MembershipService}.
 */
public class MembershipServiceFactory {

  public static MembershipService buildMembershipService(List<URI> peers, PublicKey publicKey, PrivateKey privateKey,
                                                         int gossipPort, int messageSyncPort, String id) throws IOException {
    return new ApacheGossipMembershipService(peers, publicKey, privateKey, gossipPort, messageSyncPort, id);
  }
}
