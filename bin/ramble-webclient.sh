#!/usr/bin/env bash

if [ -z "$JAVA_HOME" ]; then
  echo "Environment variable JAVA_HOME not set!"
  exit 1
fi

RAMBLE_FWDIR="$(cd `dirname $0`/..; pwd)"

for jar in $(ls -d $RAMBLE_FWDIR/lib/*); do
  RAMBLE_JARS+=":$jar"
done

$JAVA_HOME/bin/java -Dlog4j.configuration=file://$RAMBLE_FWDIR/conf/ramble-log4j.properties -cp $RAMBLE_JARS ramble.webclient.RambleClient "$@"
