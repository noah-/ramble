package ramble.api;

import java.util.List;


/**
 * A service that provides membership lists for a Ramble cluster.
 */
public interface MembershipService {

  void start();

  void shutdown();

  List<RambleMember> getMembers();
}
