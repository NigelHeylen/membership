#!/usr/bin/env bash

ENVIRONMENT=$1
clojure -Adev -m datomic.ion.dev '{:op :deploy, :group datomic-test-Compute-FHU9OX6RFQEX, :rev "'`git rev-parse HEAD`'", :uname "'`git rev-parse HEAD`'", :creds-profile "rs'$ENVIRONMENT'", :region "eu-central-1"}'