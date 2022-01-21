#!/usr/bin/env bash

argc="$#"
if [ "$argc" -eq 0 ]; then
  echo "Usage: dbinit <dbName>"
  exit 1
elif [ "$argc" -gt 1 ]; then
  echo "Only one database allowed at a time. Not doing anything"
  exit 1
fi

docker-compose run dbinit postgres/init-db.sh $1
