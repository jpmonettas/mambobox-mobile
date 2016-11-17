(ns mambobox-mobile.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [mambobox-mobile.events :refer [find-next-prev-song-id]]))


(reg-sub
  :songs
  (fn [db _]
    (->> (:songs db)
         (map (fn [[sid s]]
                [sid
                 (if (contains? (into #{} (:favourites-songs-ids db)) sid)
                   (assoc s :favourite? true)
                   s)]))
         (into {}))))

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
   (map songs f-ids)))

(reg-sub
 :hot-songs-ids-and-scores
  (fn [db _]
    (:hot-songs-ids-and-scores db)))

(reg-sub
 :hot-songs
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:hot-songs-ids-and-scores])])
 (fn [[songs h-ids] _]
   (map (fn [[id score]]
          (-> (get songs id)
              (assoc :score score)))
        h-ids)))

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
   (map songs us-ids)))

(reg-sub
 :all-artists
  (fn [db _]
    (:all-artists db)))

(reg-sub
 :selected-artist
 (fn [db _]
   (:selected-artist db)))

(reg-sub
 :selected-album-songs
 (fn [_ _]
   [(subscribe [:songs])
    (subscribe [:selected-artist])])
 (fn [[songs selected-artist] _]
   (let [selected-album-songs-ids (-> selected-artist :selected-album :songs-ids)]
     (if (pos? (count selected-album-songs-ids))
       (map songs selected-album-songs-ids)
       []))))

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
     (let [selected-tag-songs (map songs (:selected-tag-songs-ids selected-tag))]
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
   (get songs psi)))

(reg-sub
 :next-song
 (fn [db _]
   (find-next-prev-song-id db :next)))

(reg-sub
 :prev-song
 (fn [db _]
   (find-next-prev-song-id db :prev)))

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

(reg-sub
 :edit-song-dialog
 (fn [db _]
   (:edit-song-dialog (:ui db))))


