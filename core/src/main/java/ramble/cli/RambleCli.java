package ramble.cli;

import com.google.common.base.Splitter;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.log4j.Logger;

import ramble.api.Ramble;
import ramble.core.RambleImpl;
import ramble.gossip.api.IncomingMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class RambleCli {

  private static final Logger LOG = Logger.getLogger(RambleCli.class);

  private final Ramble ramble;
  private final File dumpFile;

  private RambleCli(String args[]) throws InterruptedException, IOException, URISyntaxException, ParseException {
    Options options = new Options();
    options.addOption("p", "peers", true, "Comma-separated list of initial peers to connect to");
    options.addOption("u", "port", true, "URI for Gossip service");
    options.addOption("f", "dumpfile", true, "File to dump all received messages into");
    options.addOption("h", "help", true, "Prints out CLI options");

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
    }

    URI gossipURI;
    if (cmd.hasOption('u')) {
      gossipURI = URI.create("udp://127.0.0.1:" + cmd.getOptionValue('u'));
    } else {
      gossipURI = URI.create("udp://127.0.0.1:50000");
    }

    List<String> peers;
    if (cmd.hasOption('p')) {
      peers = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(cmd.getOptionValue('p'));
    } else {
      peers = new ArrayList<>();
    }

    this.ramble = new RambleImpl(gossipURI, peers);
    this.dumpFile = new File(cmd.getOptionValue('f'));
  }

  private void run() {
    Thread dumpThread = new Thread() {
      @Override
      public void run() {
        IncomingMessage message;
        BufferedWriter writer;

        try {
          writer = new BufferedWriter(new FileWriter(dumpFile));
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }

        try {
          while ((message = ramble.listen().take()) != null) {
            writer.write("Incoming Message: " + message.getSenderId() + " " + message.getMessage());
          }
        } catch (InterruptedException | IOException e) {
          e.printStackTrace();
        }
      }
    };
    dumpThread.setDaemon(true);
    dumpThread.setUncaughtExceptionHandler((t, e) -> LOG.error("Exception while writing messages to the file "
            + dumpFile, e));
    dumpThread.start();

    Scanner sc = new Scanner(System.in);
    while (true) {
      System.out.print("Post Message: ");
      this.ramble.post(sc.nextLine());
    }
  }

  public static void main(String args[]) throws InterruptedException, IOException, URISyntaxException, ParseException {
    new RambleCli(args).run();
  }
}
