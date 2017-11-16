package ramble.crypto;

import java.net.URI;

public class URIUtils {

  public static String uriToId(URI uri) {
    return uri.getHost() + "-" + uri.getPort();
  }
}
