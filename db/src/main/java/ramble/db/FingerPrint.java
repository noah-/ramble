package ramble.db;

public class FingerPrint {
    public final byte[] sourceID;
    public final byte[] publicKey;
    public final int count;
    public final long tsStart;
    public final long tsEnd;

    public FingerPrint(byte[] sid, byte[] pk, int c, long s, long e) {
        sourceID = sid;
        publicKey = pk;
        count = c;
        tsStart = s;
        tsEnd = e;
    }
}
