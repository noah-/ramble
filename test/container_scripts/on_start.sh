#!/bin/bash

# Create Per Container Folder
file=/testdata/start.sh

while [ ! -f "$file" ]
do
    inotifywait -qqt 2 -e create -e moved_to "$(dirname $file)"
done
/testdata/start.sh

