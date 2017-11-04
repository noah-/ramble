# RAMBLE: Reliable Asynchronous Messaging for Byzantine Linked Entities

How to build: `./gradlew clean build`

How to run:

```
./gradlew clean build
tar -xvf distribution-0.0.1.tar.gz
cd ramble-distribution
./bin/ramble-cli.sh -f /tmp/ramble-messages.txt
```

This creates one instance of the RAMBLE service that uses port 50000. It does not connect to any peers. All received messages will be dumped to the file `/tmp/ramble-messages.txt`

To launch a RAMBLE service that connects to pre-existing peers, run the following command:

```
./bin/ramble-cli.sh -f /tmp/ramble-messages-2.txt -p udp://127.0.0.1:50000 -u 50001
```

This creates another instance of the RAMBLE service with peer udp://127.0.0.1:50000, and it uses port 50001.

When the RAMBLE service starts up, the CLI will have a prompt that looks like:

```
Post Message:
```

Type in a message and press `ENTER` to post a message.

All service logs are printed to the file `ramble.log`.
