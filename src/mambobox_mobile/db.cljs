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
                                          :db.player-status/collapsed?]
                                 :opt-un [:db.player/playing-song-duration
                                          :db.player-status/playing-song-id
                                          :db.player/playing-song-progress]))

(s/def :db.ui/selected-tab integer?)
(s/def :db/ui (s/keys :opt-un [:db.ui/selected-tab]))

(s/def :db/hot-songs-ids (s/coll-of :db/id))
(s/def :db/user-uploaded-songs-ids (s/coll-of :db/id))
(s/def :db/favourites-songs-ids (s/coll-of :db/id))
(s/def :db/songs (s/coll-of :mb/song))
(s/def :db/all-artists (s/coll-of :mb.song/artist))
(s/def :db/artist-albums (s/coll-of :mb.song/album))
(s/def :db/selected-album (s/keys :req [:db/id
                                        :mb.album/name]
                                  :opt-un [:db/songs]))
(s/def :db/selected-artist (s/keys :req [:db/id
                                         :mb.artist/name]
                                   :opt-un [:db/artist-albums
                                            :db/selected-album]))
(s/def :db.song/tag string?)
(s/def :db/selected-tag-songs-ids (s/coll-of :db/id))
(s/def :db/selected-tag (s/keys :req-un [:db.song/tag]
                                :opt-un [:db/selected-tag-songs-ids]))
(s/def :db.uploading.song/name string?)
(s/def :db.uploading.song/notification-id integer?)
(s/def :db.uploading/song (s/keys :req-un [:db.uploading.song/name
                                           :db.uploading.song/notification-id]))

(s/def :db.uploading.song/path string?)

(s/def :db/uploading (s/map-of :db.uploading.song/path :db.uploading/song))

(s/def ::db (s/keys :req-un [:db/player-status
                             :db/ui
                             :db/hot-songs-ids
                             :db/favourites-songs-ids
                             :db/user-uploaded-songs-ids
                             :db/songs
                             :db/all-artists]
                    :opt-un [:db/uploading
                             :db/selected-artist
                             :db/selected-tag]))

;; initial state of app-db
(def app-db {:player-status {:paused? true
                             :collapsed? true}
             :ui {:selected-tab 0}
             :hot-songs-ids #{}
             :favourites-songs-ids #{}
             :user-uploaded-songs-ids #{}
             :all-artists #{}
             :songs #{}})
