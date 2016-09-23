(ns mambobox-mobile.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))


(reg-sub
  :songs
  (fn [db _]
    (:songs db)))

(reg-sub
  :favorites-songs-ids
  (fn [db _]
    (:favorites-songs-ids db)))

(reg-sub
 :favorites-songs
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:favorites-songs-ids])])
 (fn [[songs f-ids] _]
   (filter (fn [s]
             (contains? (into #{} f-ids) (:mb.song/id s)))
           songs)))

(reg-sub
 :hot-songs-ids
  (fn [db _]
    (:hot-songs-ids db)))

(reg-sub
 :hot-songs
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:hot-songs-ids])])
 (fn [[songs h-ids] _]
   (filter (fn [s]
             (contains? (into #{} h-ids) (:mb.song/id s)))
           songs)))


(reg-sub
 :player-status
  (fn [db _]
    (:player-status db)))

(reg-sub
 :playing-song-id
  (fn [db _]
    (-> db :player-status :playing-song-id)))

(reg-sub
 :playing-song
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:playing-song-id])])
 (fn [[songs psi] _]
   (first (filter (fn [s]
                    (= psi (:mb.song/id s)))
                  songs))))
;;;;;;;;;;;;;
;; UI subs ;;
;;;;;;;;;;;;;

(reg-sub
 :selected-tab
  (fn [db _]
    (-> db :ui :selected-tab)))

