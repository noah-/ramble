package ramble.messagesync;

import com.google.common.collect.Lists;
import ramble.api.RambleMember;
import ramble.messagesync.api.TargetSelector;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RandomTargetSelector implements TargetSelector {

  @Override
  public RambleMember getTarget(Set<RambleMember> members) {
    return getTargets(members, 1).iterator().next();
  }

  @Override
  public Set<RambleMember> getTargets(Set<RambleMember> members, int numTargets) {
    List<RambleMember> memberList = Lists.newArrayList(members);
    Collections.shuffle(memberList);
    return new HashSet<>(memberList.subList(0, Math.min(numTargets, memberList.size())));
  }
}
