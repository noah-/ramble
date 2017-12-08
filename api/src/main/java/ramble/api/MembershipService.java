package ramble.api;

import com.google.common.util.concurrent.Service;

import java.util.Set;


/**
 * A service that provides membership lists for a Ramble cluster.
 */
public interface MembershipService extends Service {

  Set<RambleMember> getMembers();
}
