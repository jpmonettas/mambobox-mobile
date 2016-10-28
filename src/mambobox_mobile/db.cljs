(ns mambobox-mobile.db
  (:require [clojure.spec :as s]
            [mambobox-core.core-spec]))

;; spec of app-db
(s/def :db.player-status/paused? boolean?)
(s/def :db.player-status/collapsed? boolean?)
(s/def :db.player-status/playing-song-id :db/id)
(s/def :db.player/playing-song-duration any?)
(s/def :db.player/playing-song-progress any?)
(s/def :db/player-status (s/keys :req-un [:db.player-status/paused?
                                          :db.player-status/collapsed?
                                          :db.player-status/playing-song-id]
                                 :opt-un [:db.player/playing-song-duration
                                          :db.player/playing-song-progress]))
(s/def :db/ui any?)
(s/def :db/hot-songs (s/coll-of :db/id))
(s/def :db/favorites-songs (s/coll-of :db/id))
;; (s/def :db/songs (s/coll-of :mb/song))

(s/def :db.uploading.song/name string?)
(s/def :db.uploading.song/notification-id integer?)
(s/def :db.uploading/song (s/keys :req-un [:db.uploading.song/name
                                           :db.uploading.song/notification-id]))

(s/def :db.uploading.song/path string?)

(s/def :db/uploading (s/map-of :db.uploading.song/path :db.uploading/song))

(s/def ::db any? #_(s/keys :req-un [:db/player-status
                             :db/ui
                             :db/hot-songs-ids
                             :db/favorites-songs-ids
                             :db/songs]
                    :opt-un [:db/uploading]))

;; initial state of app-db
(def app-db {:player-status {:paused? true
                             :collapsed? true}
             :ui {:selected-tab 0}
             :hot-songs-ids #{}
             :favourites-songs-ids #{}
             :songs #{}})
