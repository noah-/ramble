package ramble.db;

public class BlockInfo {

    private final long timestamp;
    private final int count;

    public BlockInfo(long timestamp, int count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int getCount() {
        return this.count;
    }
}
