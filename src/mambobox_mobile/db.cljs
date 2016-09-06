(ns mambobox-mobile.db
  (:require [clojure.spec :as s]
            [devtools.core :as devt]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::counter integer?)
(s/def ::playing boolean?)
(s/def ::app-db
  (s/keys :req-un [::greeting ::counter ::playing]))

;; initial state of app-db
(def app-db {:greeting "Hello Clojure in iOS and Android!"
             :counter 0
             :playing false})
