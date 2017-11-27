#!/bin/bash

sudo docker build -t test0 .

tc qdisc add dev eth0 root netem delay 20ms

for number in {1..10}
do
#sudo docker run --net testnet --ip 172.20.128.$number  -it --rm -v $(pwd)/testresult:/testdata test0  sh 
sudo docker run --net testnet --ip 172.20.128.$number  -it --rm -v $(pwd)/testresult:/testdata --cap-add=NET_ADMIN -d=true test0 # sh 
done

