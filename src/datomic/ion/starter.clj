;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns datomic.ion.starter
  (:require
    [clojure.data.json :as json]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pp]
    [datomic.client.api :as d]
    [datomic.ion.lambda.api-gateway :as apigw]
    [cognitect.transit :as transit])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))


(defn write-transit [x]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer x)
    (.toString out)))

(defn read-transit [x]
  (let [in (ByteArrayInputStream. (.getBytes x))
        reader (transit/reader in :json)]
    (transit/read reader)))

(def get-client
  "This function will return a local implementation of the client
interface when run on a Datomic compute node. If you want to call
locally, fill in the correct values in the map."
  (memoize #(d/client {:server-type :ion
                       :region      "eu-central-1" ;; e.g. us-east-1
                       :system      "datomic-test"
                       :endpoint    "http://entry.datomic-test.eu-central-1.datomic.net:8182/"
                       :proxy-port 8182})))

(defn- anom-map
  [category msg]
  {:cognitect.anomalies/category (keyword "cognitect.anomalies" (name category))
   :cognitect.anomalies/message msg})

(defn- anomaly!
  ([name msg]
     (throw (ex-info msg (anom-map name msg))))
  ([name msg cause]
     (throw (ex-info msg (anom-map name msg) cause))))

(defn ensure-dataset
  "Ensure that a database named db-name exists, running setup-fn
against a connection. Returns connection"
  [db-name setup-sym]
  (require (symbol (namespace setup-sym)))
  (let [setup-var (resolve setup-sym)
        client (get-client)]
    (when-not setup-var
      (anomaly! :not-found (str "Could not resolve " setup-sym)))
    (d/create-database client {:db-name db-name})
    (let [conn (d/connect client {:db-name db-name})
          db (d/db conn)]
      (setup-var conn)
      conn)))

(defn modes
  "Query aggregate fn that returns the set of modes for a collection."
  [coll]
  (->> (frequencies coll)
       (reduce
        (fn [[modes ct] [k v]]
          (cond
           (< v ct)  [modes ct]
           (= v ct)  [(conj modes k) ct]
           (> v ct) [#{k} v]))
        [#{} 2])
       first))

(defn pp-str
  [x]
  (binding [*print-length* nil
            *print-level* nil]
    (with-out-str (pp/pprint x))))

(defn get-connection
  []
  (ensure-dataset "datomic-membership-13-12-2018"
                  'datomic.ion.starter.examples.tutorial/load-dataset))

(defn schema
  "Returns a data representation of db schema."
  [db]
  (->> (d/pull db '{:eid 0 :selector [{:db.install/attribute [*]}]})
       :db.install/attribute
       (map #(update % :db/valueType :db/ident))
       (map #(update % :db/cardinality :db/ident))))


;; Ions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tutorial-schema-handler
  "Web handler that returns the schema for datomic-docs-tutorial"
  [{:keys [headers body]}]
  {:status 200
   :headers {"Content-Type" "application/edn"} 
   :body (-> (get-connection) d/db schema pp-str)})

(def get-tutorial-schema
  "API Gateway web service ion for tutorial-schema-handler."
  (apigw/ionize tutorial-schema-handler))

(defn echo
  "Lambda ion that simply echoes its input"
  [{:keys [context input]}]
  input)

(defn read-edn
  [input-stream]
  (some-> input-stream io/reader (java.io.PushbackReader.) edn/read))



(defn start-training*
  "Transaction fn that creates data to make a new item"
  [db type id]
  [{:object/id id
    :training/start (java.util.Date.)
    :training/type (keyword type)}])

(defn start-training
  "Lambda ion that starts a training, returns a training-id."
  [{:keys [input]}]
  (let [args (if (keyword? type) type
                                 (-> input json/read-str keyword))
        conn (get-connection)
        training-id (java.util.UUID/randomUUID)
        tx [(list* 'datomic.ion.starter/start-training* [args training-id])]
        _ (d/transact conn {:tx-data tx})]
    (write-transit {:id training-id})))

(defn feature-item?
  "Query ion exmaple. This predicate matches entities that
should be featured in a promotion."
  [db e]
  ;;  While this particular predicate could also be implemented as
  ;; additional clauses in query, your own programs can do anything
  ;; they want here!
  (let [{:keys [inv/color inv/size inv/type]} (d/pull db {:eid e :selector [:inv/color :inv/size :inv/type]})]
    (and (= (:db/ident color) :green)
         (= (:db/ident size) :xlarge)
         (= (:db/ident type) :hat))))


(defn start-training-web*
  "Lambda ion that returns sample database items matching type."
  [{:keys [headers body]}]
  (let [type (some-> body read-edn)]
    (if (keyword? type)
      {:status 200
       :headers {"Content-Type" "application/edn"}
       :body (start-training {:input type})}
      {:status 400
       :headers {}
       :body "Expected a request body keyword naming a type"})))

(def start-training-web
  "API Gateway web service ion for start-training"
  (apigw/ionize start-training-web*))