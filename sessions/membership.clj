(ns membership
  (:require [datomic.ion.starter :as starter]
            [datomic.client.api :as d]
            [cognitect.transit :as transit]
            [clojure.data.json :as json])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))


(def conn (starter/get-connection))

(def db (d/db conn))

(def tx-data [(starter/start-training* db :training.type/gi (java.util.UUID/randomUUID))])


(comment
  (starter/start-training {:input "\"training.type/gi\""}))

(java.util.UUID/fromString "04562b6a-1420-4afb-aea9-0317e02fa01d")
(d/q '[:find (pull ?t [:*])
       :in $ ?id
       :where [?t :object/id ?id]] db (java.util.UUID/fromString "04562b6a-1420-4afb-aea9-0317e02fa01d"))

(def out (ByteArrayOutputStream. 4096))
(def writer (transit/writer out :json))


(def in (ByteArrayInputStream. (.toByteArray out)))
(def reader (transit/reader in :json))

(type (:id (transit/read reader)))