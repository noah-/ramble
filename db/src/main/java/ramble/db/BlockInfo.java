package ramble.db;

public class BlockInfo {
    private long timeStamp;
    private int count;

    public BlockInfo(long ts, int c) {
        timeStamp = ts;
        count = c;
    }

    public long getTimeStamp() { return timeStamp; }
    public int getCount() { return count; }
}
