#!/bin/bash
#tc qdisc add dev eth0 root netem delay 20ms

for number in {1..64}
do
#sudo docker run --net testnet --ip 172.20.128.$number  -it --rm -v $(pwd)/testresult:/testdata test0  sh 
sudo docker run --net testnet --name ramble-$number --ip 172.20.128.$number  -it --rm -v $(pwd):/testroot --cap-add=NET_ADMIN -d=true ramble /testroot/on_start.sh
done

