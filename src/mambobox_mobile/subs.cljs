(ns mambobox-mobile.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :favorites-songs
  (fn [db _]
    (:favorites-songs db)))

(reg-sub
 :player-status
  (fn [db _]
    (select-keys db [:playing])))
