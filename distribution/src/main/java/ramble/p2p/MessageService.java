package ramble.p2p;

import java.util.HashSet;

public class MessageService {
    public static HashSet<String> hs0 = null;
    public static HashSet<String> hs1 = null;

    private int id;

    public MessageService(int id) {
        this.id = id;
    }

    public void sendBlock(HashSet<String> hs) { // integrate this directly with netty
        if (id == 0)
            hs0 = hs;
        else
            hs1 = hs;
    }

    public HashSet<String> getBlock() throws InterruptedException{ // same here - any paraeter for connection peer uri
        HashSet<String> hs;

        if (id == 0)
            hs = hs1;
        else
            hs = hs0;

        while (hs == null) {
            Thread.sleep(10);
            if (id == 0)
                hs = hs1;
            else
                hs = hs0;
        }

        return hs;
    }
}
