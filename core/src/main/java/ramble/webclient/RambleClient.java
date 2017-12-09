package ramble.webclient;

import static spark.Spark.*;
import spark.*;

import com.google.common.base.Splitter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import ramble.api.Ramble;
import ramble.api.RambleMessage;
import ramble.core.RambleImpl;
import ramble.crypto.KeyReader;
import spark.template.velocity.*;

public class RambleClient {

  private static final Logger LOG = Logger.getLogger(RambleClient.class);

  private final Ramble ramble;
  private final File dumpFile;

  private RambleClient(String args[])
          throws IOException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException {
    // Parse the CLI options
    Options options = new Options();
    options.addOption("p", "peers", true, "Comma-separated list of initial peers to connect to");
    options.addOption("u", "gossip-port", true, "URI for Gossip service");
    options.addOption("m", "message-sync-port", true, "URI for Message Sync service");
    options.addOption("f", "dumpfile", true, "File to dump all received messages into");
    options.addOption("h", "help", false, "Prints out CLI options");
    options.addOption("pu", "publickey", true, "Path to public key file");
    options.addOption("pr", "privatekey", true, "Path to private key file");

    CommandLineParser parser = new BasicParser();
    CommandLine cmd = parser.parse(options, args);

    HelpFormatter formatter = new HelpFormatter();

    if (cmd.hasOption('h')) {
      formatter.printHelp("ramble-cli", options);
      System.exit(0);
    }

    if (!cmd.hasOption('f')) {
      System.out.println("Missing required option -f!");
      formatter.printHelp("ramble-cli", options);
      System.exit(0);
    }

    String pkpath;
    if (cmd.hasOption("pu")) {
      pkpath = cmd.getOptionValue("pu");
    } else {
      pkpath = "/tmp/ramble-key-store/ramble-cli.pub";
    }

    String skpath;
    if (cmd.hasOption("pr")) {
      skpath = cmd.getOptionValue("pr");
    } else {
      skpath = "/tmp/ramble-key-store/ramble-cli";
    }

    int gossipPort;
    if (cmd.hasOption('u')) {
      gossipPort = Integer.parseInt(cmd.getOptionValue('u'));
    } else {
      gossipPort = 5000;
    }

    int messageSyncPort;
    if (cmd.hasOption('m')) {
      messageSyncPort = Integer.parseInt(cmd.getOptionValue('m'));
    } else {
      messageSyncPort = 5001;
    }

    List<URI> peers;
    if (cmd.hasOption('p')) {
      peers = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(cmd.getOptionValue('p')).stream().map(
              URI::create).collect(Collectors.toList());
    } else {
      peers = new ArrayList<>();
    }

    KeyReader keyReader = new KeyReader();
    PublicKey publicKey = keyReader.getPublicKey(Paths.get(pkpath));
    PrivateKey privateKey = keyReader.getPrivateKey(Paths.get(skpath));

    // Create the RAMBLE service
    this.ramble = new RambleImpl(peers, publicKey, privateKey, gossipPort, messageSyncPort);
    this.dumpFile = new File(cmd.getOptionValue('f'));

  }

  private List<RambleMessage.Message> GetMessageSet(String parent) {
    List<RambleMessage.Message> ret = new ArrayList<RambleMessage.Message>();
    for (RambleMessage.Message msg : this.ramble.getAllMessages()) {
      if (msg.getMessage().split(":")[1].equals(parent)) {
        ret.add(msg);
      }
    }
    return ret;
  }

  private void run() {
    this.ramble.start();
    Runtime.getRuntime().addShutdownHook(new Thread(){
      @Override
      public void run() {
        ramble.shutdown();
      }
    });

    staticFiles.location("/public");

    get("/postmessage/*", (req, res) -> {
      if(req.queryParams("msg") != null) {
        this.ramble.post(UUID.randomUUID() +":" + req.splat()[0]
            + ":" + req.queryParams("msg"));
      }
      res.redirect("/message/" + req.splat()[0]);
      return "Not important";
    });


    get("/allmessage", (req, res) -> {
      Map<String, Object> model = new HashMap();
      model.put("msgs", this.GetMessageSet("0"));
      model.put("this_msg_id", "0");
      model.put("header", "All Top Messages");
     return new VelocityTemplateEngine().render(new ModelAndView(model, "template/message.vm"));
    });

    get("/message/*", (req, res) -> {
      Map<String, Object> model = new HashMap();
      model.put("msgs",this.GetMessageSet(req.splat()[0]));
      model.put("this_msg_id", req.splat()[0]);
      model.put("header", "reply for message");
      return new VelocityTemplateEngine().render(new ModelAndView(model, "template/message.vm"));
    });
  }

  public static void main(String args[]) throws IOException, ParseException, NoSuchAlgorithmException,
          InvalidKeySpecException {
    new RambleClient(args).run();
  }
}
