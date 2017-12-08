package ramble.api;

import java.util.Set;


/**
 * A service that provides membership lists for a Ramble cluster.
 */
public interface MembershipService {

  void start();

  void shutdown();

  Set<RambleMember> getMembers();
}
