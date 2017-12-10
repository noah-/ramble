#!/bin/bash

for number in {1..10}
do

sudo docker exec ramble-$number tc qdisc add dev eth0 root netem delay 20ms
done

