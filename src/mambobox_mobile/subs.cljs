(ns mambobox-mobile.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :get-greeting
  (fn [db _]
    (str (:greeting db) " " (:counter db))))

(reg-sub
 :player-status
  (fn [db _]
    (select-keys db [:playing])))
