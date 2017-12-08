package ramble.messagesync;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

public class AntiEntropyService extends AbstractScheduledService implements Service {

  // TODO schedule AntiEntropy.java to run in this class and connect to RambleImpl

  @Override
  protected void runOneIteration() throws Exception {

  }

  @Override
  protected Scheduler scheduler() {
    return null;
  }
}
