;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns datomic.ion.starter.examples.tutorial
  (:require
   [datomic.client.api :as d]
   [datomic.ion.starter :as starter]))

(defn make-idents
  [x]
  (mapv #(hash-map :db/ident %) x))


(def member-schema
  [{:db/ident :person/first-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/last-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/birthdate
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/memberships
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :person/current-membership
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def membership-schema
  [{:db/ident :membership/start
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :membership/end
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :membership/sessions-count
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :membership/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def membership-type [:membership.type/subscription :membership.type/session])

(def training-schema
  [{:db/ident :training/start
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :training/end
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :training/attendees
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :training/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :training/subject
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(def training-type [:training.type/gi :training.type/no-gi])

(defn- has-ident?
  [db ident]
  (contains? (d/pull db {:eid ident :selector [:db/ident]})
             :db/ident))

(defn- data-loaded?
  [db]
  (has-ident? db :inv/sku))

(defn load-dataset
  [conn]
  (let [db (d/db conn)]
    (if (data-loaded? db)
      :already-loaded
      (let [xact #(d/transact conn {:tx-data %})]
        (xact (make-idents membership-type))
        (xact member-schema)
        (xact membership-schema)
        (xact training-schema)
        :loaded))))
