#!/bin/sh

cd /usr/src/ramble/ramble-distribution
./bin/ramble-cli-setup-keys.sh
./bin/ramble-cli-setup-db.sh 5000 5001

# Create Per Container Folder
file=/testdata/start.sh
tc qdisc add dev eth0 root netem delay 20ms

./bin/ramble-cli.sh -f /testroot/testresult/ramble-messages-$HOSTNAME.txt -pu /tmp/ramble-key-store/ramble-cli.pub -pr /tmp/ramble-key-store/ramble-cli -p udp://172.20.128.1:5000

#while [ ! -f "$file" ]
#do
#    inotifywait -qqt 2 -e create -e moved_to "$(dirname $file)"
#done

#/testdata/start.sh

