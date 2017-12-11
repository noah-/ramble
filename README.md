# RAMBLE: Reliable Asynchronous Messaging for Byzantine Linked Entities

How to build: `./gradlew clean assemble`

How to run:

```
./gradlew clean assemble
tar -xvf distribution-0.0.1.tar.gz
cd ramble-distribution
./bin/ramble-cli.sh -f /tmp/ramble-messages.txt
```

This creates one instance of the RAMBLE service that uses port `5000` for a Gossip membership service, and port `6000` for a message sync service. It does not connect to any peers. All received messages will be dumped to the file `/tmp/ramble-messages.txt`

To launch a RAMBLE service that connects to pre-existing peers, run the following command:

```
./bin/ramble-cli.sh -f /tmp/ramble-messages-2.txt -p ramble://[ip-address-of-first-node]:5000 -u 5001 -b ramble://[ip-address-of-first-node]:6000 -mm 6001
```

The value of `ip-address-of-first-node` can be find from the `ramble.log` file of the first node.

This creates another instance of the RAMBLE service with peer `ramble://[ip-address-of-first-node]:5000`, and it uses port `5001` for Gossip and port `6001` for message-sync.

When the RAMBLE service starts up, the CLI will have a prompt that looks like:

```
Post Message:
```

Type in a message and press `ENTER` to post a message.

All service logs are printed to the file `ramble.log`.

The current CLI options fot the `./bin/ramble-cli.sh` script are:

```
usage: ramble-cli
 -b,--bootstrap-node <arg>      The node to bootstrap against of the form
                                ramble://[target-ip-address]:[target-messa
                                ge-sync-port]
 -f,--dumpfile <arg>            File to dump all received messages into
 -h,--help                      Prints out CLI options
 -m,--message-sync-port <arg>   URI for Message Sync service
 -p,--peers <arg>               Comma-separated list of initial Gossip
                                peers to connect to, of the form
                                ramble://[target-ip-address]:[target-gossi
                                p-port]
 -pr,--privatekey <arg>         Path to private key file
 -pu,--publickey <arg>          Path to public key file
 -u,--gossip-port <arg>         URI for Gossip service
```
