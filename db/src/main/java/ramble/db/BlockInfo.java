package ramble.db;

public class BlockInfo {
    public final long timeStamp;
    public final int count;

    public BlockInfo(long ts, int c) {
        timeStamp = ts;
        count = c;
    }
}
