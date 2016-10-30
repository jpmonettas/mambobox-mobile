(ns mambobox-mobile.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))


(reg-sub
  :songs
  (fn [db _]
    (map (fn [s]
           (if (contains? (into #{} (:favourites-songs-ids db)) (:db/id s))
             (assoc s :favourite? true)
             s))
     (:songs db))))

(reg-sub
  :favourites-songs-ids
  (fn [db _]
    (:favourites-songs-ids db)))

(reg-sub
 :favourites-songs
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:favourites-songs-ids])])
 (fn [[songs f-ids] _]
   (filter (fn [s]
             (contains? (into #{} f-ids) (:db/id s)))
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
             (contains? (into #{} h-ids) (:db/id s)))
           songs)))

(reg-sub
 :user-uploaded-songs-ids
  (fn [db _]
    (:user-uploaded-songs-ids db)))

(reg-sub
 :user-uploaded-songs
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:user-uploaded-songs-ids])])
 (fn [[songs us-ids] _]
   (filter (fn [s]
             (contains? (into #{} us-ids) (:db/id s)))
           songs)))

(reg-sub
 :all-artists
  (fn [db _]
    (:all-artists db)))

(reg-sub
 :selected-artist
  (fn [db _]
    (:selected-artist db)))

(reg-sub
 :selected-tag
  (fn [db _]
    (:selected-tag db)))

(reg-sub
 :full-selected-tag
 (fn [db _]
   [(subscribe [:songs])
    (subscribe [:selected-tag])])
 (fn [[songs selected-tag] _]
   (when selected-tag
     (let [selected-tag-songs (filter (fn [s]
                                       (contains? (:selected-tag-songs-ids selected-tag) (:db/id s)))
                                     songs)]
      (assoc selected-tag :songs selected-tag-songs)))))

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
                    (= psi (:db/id s)))
                  songs))))
;;;;;;;;;;;;;
;; UI subs ;;
;;;;;;;;;;;;;

(reg-sub
 :selected-tab
  (fn [db _]
    (-> db :ui :selected-tab)))

(reg-sub
 :searching
 (fn [db _]
   (:searching db)))
