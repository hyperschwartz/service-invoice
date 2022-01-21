#!/usr/bin/env bash

logPrefix="JAVASCRIPT PROTO GENERATION:"
echo "$logPrefix Generating output directories"

mkdir -p ./build/generated/source/proto/main/ts
mkdir -p ./build/generated/source/proto/main/js

echo "$logPrefix Generating Typescript protos"

proto_dirs=$(find ./src/main/proto -path -prune -o -name '*.proto' -print0 | xargs -0 -n1 dirname | sort | uniq)
for dir in $proto_dirs; do
  protoc \
  -I "./src/main/proto" \
  --plugin=protoc-gen-ts=./node_modules/.bin/protoc-gen-ts \
  --ts_out="build/generated/source/proto/main/ts" \
  --js_out=import_style=commonjs,binary:./build/generated/source/proto/main/ts \
  $(find "${dir}" -maxdepth 1 -name '*.proto')
done

echo "$logPrefix Generation Complete"
