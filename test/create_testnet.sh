#!/bin/bash

## Create network.
sudo docker network create --driver overlay --subnet 172.20.0.0/16 --ip-range 172.20.240.0/20  --attachable testnet

