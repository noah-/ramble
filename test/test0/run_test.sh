#!/bin/bash

sudo docker build -t test0 .

for number in {10..20}
do
#sudo docker run --net testnet --ip 172.20.128.$number  -it --rm -v $(pwd)/testresult:/testdata test0  sh 
sudo docker run --net testnet --ip 172.20.128.$number  -it --rm -v $(pwd)/testresult:/testdata -d=true test0 # sh 
done

