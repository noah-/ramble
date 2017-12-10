package ramble.messagesync.api;

import ramble.api.RambleMember;

import java.util.Optional;
import java.util.Set;

public interface TargetSelector {

  Optional<RambleMember> getTarget(Set<RambleMember> members);

  Set<RambleMember> getTargets(Set<RambleMember> members, int numTargets);
}
