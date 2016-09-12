(ns mambobox-mobile.db
  (:require [clojure.spec :as s]))

;; spec of app-db

(s/def ::app-db any?)

;; initial state of app-db
(def app-db {:player-status {:paused true
                             :playing-song {:song-name "Por tu amor"
                                            :artist-name "Latin Vibe"
                                            :duration 411
                                            :url "http://192.168.1.8:9999/resources/music/Por_tu_amor.mp3"}}
             :ui {:selected-tab 0}
             :hot-songs [{:song-name "Guguanco del gran combo"
                          :artist-name "El gran combo de puerto rico"
                          :duration 410
                          :url "http://192.168.1.8:9999/resources/music/Guaguanco.mp3"}
                         {:song-name "Por tu amor"
                          :artist-name "Latin Vibe"
                          :duration 411
                          :url "http://192.168.1.8:9999/resources/music/Por_tu_amor.mp3"}
                         {:song-name "La llave"
                          :artist-name "Latin Vibe"
                          :duration 412
                          :url "http://192.168.1.8:9999/resources/music/La_llave.mp3"}]
             :favorites-songs [{:song-name "Guguanco del gran combo"
                          :artist-name "El gran combo de puerto rico"
                          :duration 410
                          :url "http://192.168.1.8:9999/resources/music/Guaguanco.mp3"}
                         {:song-name "Por tu amor"
                          :artist-name "Latin Vibe"
                          :duration 411
                          :url "http://192.168.1.8:9999/resources/music/Por_tu_amor.mp3"}
                         {:song-name "La llave"
                          :artist-name "Latin Vibe"
                          :duration 412
                          :url "http://192.168.1.8:9999/resources/music/La_llave.mp3"}
                               {:song-name "Super song 0" :artist-name "Mega artist a" :duration 120}
                               {:song-name "Super song 1" :artist-name "Mega artist b" :duration 121}
                               {:song-name "Super song 2" :artist-name "Mega artist c" :duration 122}
                               {:song-name "Super song 3" :artist-name "Mega artist d" :duration 123}
                               {:song-name "Super song 4" :artist-name "Mega artist e" :duration 124}
                               {:song-name "Super song 5" :artist-name "Mega artist f" :duration 125}
                               {:song-name "Super song 6" :artist-name "Mega artist g" :duration 126}
                               {:song-name "Super song 7" :artist-name "Mega artist h" :duration 127}
                               {:song-name "Super song 8" :artist-name "Mega artist i" :duration 128}
                               {:song-name "Super song 9" :artist-name "Mega artist j" :duration 129}
                               {:song-name "Super song 10" :artist-name "Mega artist k" :duration 210}
                               {:song-name "Super song 11" :artist-name "Mega artist l" :duration 211}
                               {:song-name "Super song 12" :artist-name "Mega artist m" :duration 212}
                               {:song-name "Super song 13" :artist-name "Mega artist n" :duration 213}
                               {:song-name "Super song 14" :artist-name "Mega artist o" :duration 214}]})
