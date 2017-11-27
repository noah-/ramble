#!/bin/bash

for number in {1..10}
do
ping 172.20.128.$number -c 3 >> /testdata/$HOSTNAME
done
