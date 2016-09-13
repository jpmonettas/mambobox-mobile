(ns mambobox-mobile.db
  (:require [clojure.spec :as s]
            [mambobox-core.core-spec]))

;; spec of app-db
(s/def :db.player-status/paused boolean?)
(s/def :db.player-status/playing-song :mb/song)
(s/def :db.player/playing-song-duration any?)
(s/def :db.player/playing-song-progress any?)
(s/def :db/player-status (s/keys :req-un [:db.player-status/paused
                                          :db.player-status/playing-song]
                                 :opt-un [:db.player/playing-song-duration
                                          :db.player/playing-song-progress]))
(s/def :db/ui any?)
(s/def :db/hot-songs (s/coll-of :mb/song))
(s/def :db/favorites-songs (s/coll-of :mb/song))
(s/def ::db (s/keys :req-un [:db/player-status
                             :db/ui
                             :db/hot-songs
                             :db/favorites-songs]))

;; initial state of app-db
(def app-db {:player-status {:paused true
                             :playing-song {:mb.song/name "Por tu amor"
                                            :mb.artist/name "Latin Vibe"
                                            :mb.song/duration 411
                                            :mb.song/url "http://192.168.1.8:9999/resources/music/Por_tu_amor.mp3"}}
             :ui {:selected-tab 0}
             :hot-songs [{:mb.song/name "Guguanco del gran combo"
                          :mb.artist/name "El gran combo de puerto rico"
                          :mb.song/duration 410
                          :mb.song/url "http://192.168.1.8:9999/resources/music/Guaguanco.mp3"}
                         {:mb.song/name "Por tu amor"
                          :mb.artist/name "Latin Vibe"
                          :mb.song/duration 411
                          :mb.song/url "http://192.168.1.8:9999/resources/music/Por_tu_amor.mp3"}
                         {:mb.song/name "La llave"
                          :mb.artist/name "Latin Vibe"
                          :mb.song/duration 412
                          :mb.song/url "http://192.168.1.8:9999/resources/music/La_llave.mp3"}]
             :favorites-songs [{:mb.song/name "Guguanco del gran combo"
                          :mb.artist/name "El gran combo de puerto rico"
                          :mb.song/duration 410
                          :mb.song/url "http://192.168.1.8:9999/resources/music/Guaguanco.mp3"}
                               {:mb.song/name "Por tu amor"
                          :mb.artist/name "Latin Vibe"
                          :mb.song/duration 411
                          :mb.song/url "http://192.168.1.8:9999/resources/music/Por_tu_amor.mp3"}
                               {:mb.song/name "La llave"
                          :mb.artist/name "Latin Vibe"
                          :mb.song/duration 412
                          :mb.song/url "http://192.168.1.8:9999/resources/music/La_llave.mp3"}]})
