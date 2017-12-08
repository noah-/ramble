package ramble.messagesync;

import com.google.common.collect.Lists;
import ramble.api.RambleMember;
import ramble.messagesync.api.TargetSelector;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomTargetSelector implements TargetSelector {

  private final Random rand = new Random();

  @Override
  public RambleMember getTarget(Set<RambleMember> members) {
    List<RambleMember> memberList = Lists.newArrayList(members);
    return memberList.get(this.rand.nextInt(memberList.size()));
  }
}
