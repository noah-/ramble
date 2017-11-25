package ramble.messagesync.api;

public interface MessageSyncServer {

  void start() throws InterruptedException;

  void stop();
}
