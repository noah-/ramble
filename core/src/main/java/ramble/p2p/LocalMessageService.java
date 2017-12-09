package ramble.p2p;

import java.util.Set;

public class LocalMessageService {
    public static Set<byte[]> hs0 = null;
    public static Set<byte[]> hs1 = null;

    private int id;

    public LocalMessageService(int id) {
        this.id = id;
    }

    public void sendBlock(Set<byte[]> hs) { // integrate this directly with netty
        if (id == 0)
            hs0 = hs;
        else
            hs1 = hs;
    }

    public Set<byte[]> getBlock() throws InterruptedException{ // same here - any paraeter for connection peer uri
        Set<byte[]> hs;

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
