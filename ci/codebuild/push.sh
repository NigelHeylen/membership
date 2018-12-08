#!/usr/bin/env bash

clj -A:dev -m datomic.ion.dev '{:op :push :region "eu-central-1" :uname "'`git rev-parse HEAD`'"}'