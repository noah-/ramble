package ramble.messagesync.api;

import ramble.api.RambleMember;

import java.util.Set;

public interface TargetSelector {

  RambleMember getTarget(Set<RambleMember> members);
}
