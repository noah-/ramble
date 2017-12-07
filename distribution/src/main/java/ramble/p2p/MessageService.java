package ramble.p2p;

import java.util.HashSet;

public class MessageService {
    public static HashSet<String> hs0 = null;
    public static HashSet<String> hs1 = null;

    private int id;

    public MessageService(int id) {
        this.id = id;
    }

    public void sendBlock(HashSet<String> hs) {
        HashSet<String> copy = new HashSet<String>();

        for (String s : hs) {
            copy.add(s);
        }

        if (id == 0)
            hs0 = copy;
        else
            hs1 = copy;
    }

    public HashSet<String> getBlock() throws InterruptedException{
        HashSet<String> hs;
        HashSet<String> copy = new HashSet<String>();

        if (id == 0)
            hs = hs1;
        else
            hs = hs0;

        while (hs == null) {
            Thread.sleep(10);
        }

        for (String s : hs) {
            copy.add(s);
        }

        return copy;
    }
}
