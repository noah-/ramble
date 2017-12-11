#!/bin/bash
#tc qdisc add dev eth0 root netem delay 20ms

sudo docker run --net testnet --name ramble-97 --ip 172.20.128.97  -it --rm -v $(pwd):/testroot --cap-add=NET_ADMIN -d=true ramble /testroot/on_start_spamming.sh

