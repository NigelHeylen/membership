#!/usr/bin/env bash

clojure -Adev -m datomic.ion.dev '{:op :deploy, :group datomic-test-Compute-FHU9OX6RFQEX, :rev "'`git rev-parse HEAD`'", :uname "'`git rev-parse HEAD`'", :region "eu-central-1"}'