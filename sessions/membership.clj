(ns membership
  (:require [datomic.ion.starter :as starter]
            [datomic.client.api :as d]))


(def conn (starter/get-connection))

(def db (d/db conn))

(def tx-data [(starter/start-training* db :training.type/gi (java.util.UUID/randomUUID))])


(comment
  (starter/start-training {:input "\"training.type/gi\""}))