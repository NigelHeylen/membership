(ns membership
  (:require [datomic.ion.starter :as starter]
            [datomic.client.api :as d]
            [cognitect.transit :as transit])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))


(def conn (starter/get-connection))

(def db (d/db conn))

(def tx-data [(starter/start-training* db :training.type/gi (java.util.UUID/randomUUID))])


(comment
  (starter/start-training {:input "\"training.type/gi\""}))

(def out (ByteArrayOutputStream. 4096))
(def writer (transit/writer out :json))


(transit/write writer {:id (java.util.UUID/randomUUID)})

(.toString out)


(def in (ByteArrayInputStream. (.toByteArray out)))
(def reader (transit/reader in :json))

(type (:id (transit/read reader)))