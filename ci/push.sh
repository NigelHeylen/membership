#!/usr/bin/env bash

ENVIRONMENT=$1
clj -A:dev -m datomic.ion.dev '{:op :push :creds-profile "rs'$ENVIRONMENT'" :region "eu-central-1" :uname "'`git rev-parse HEAD`'"}'