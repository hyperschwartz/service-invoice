#!/usr/bin/env bash

pushd ./local-development || exit

./local-up.sh

popd || exit
