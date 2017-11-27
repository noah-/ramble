#!/bin/sh

# Create Per Container Folder
file=/testdata/start.sh
tc qdisc add dev eth0 root netem delay 20ms

while [ ! -f "$file" ]
do
    inotifywait -qqt 2 -e create -e moved_to "$(dirname $file)"
done

/testdata/start.sh

