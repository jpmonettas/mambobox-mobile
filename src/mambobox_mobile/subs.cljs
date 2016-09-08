(ns mambobox-mobile.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :favorites-songs
  (fn [db _]
    (:favorites-songs db)))

(reg-sub
 :hot-songs
  (fn [db _]
    (:hot-songs db)))

(reg-sub
 :player-status
  (fn [db _]
    (:player-status db)))

;;;;;;;;;;;;;
;; UI subs ;;
;;;;;;;;;;;;;

(reg-sub
 :selected-tab
  (fn [db _]
    (-> db :ui :selected-tab)))

