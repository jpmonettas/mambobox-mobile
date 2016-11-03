(ns mambobox-mobile.services
  (:require [ajax.edn :as ajax-edn]
            [ajax.core :as ajax]
            [mambobox-mobile.constants :as constants]))

(def http-fxs-commons
  {:timeout 8000
   :format (ajax-edn/edn-request-format)
   :response-format (ajax-edn/edn-response-format)})

(defn register-device-http-fx [device-id locale country]
  (merge http-fxs-commons
   {:method :post
    :uri (str constants/server-url "/user/register-device?device-id=" device-id)
    :params {:locale locale
             :country country}}))

(defn get-initial-dump-http-fx [device-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/initial-dump?device-id=" device-id)}))

(defn load-artist-albums-http-fx [device-id artist-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/explore-artist?device-id=" device-id "&artist-id=" artist-id)}))

(defn load-all-artists-http-fx [device-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/all-artists?device-id=" device-id)}))

(defn load-album-songs-http-fx [device-id album-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/explore-album?device-id=" device-id "&album-id=" album-id)}))

(defn load-tag-songs-http-fx [device-id tag page]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/explore-tag?device-id=" device-id "&tag=" tag "&page=" page)}))

(defn load-hot-songs-http-fx [device-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/hot?device-id=" device-id)}))

(defn tag-song-http-fx [device-id song-id tag]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/" song-id "/tags/" tag "?device-id=" device-id)}))

(defn untag-song-http-fx [device-id song-id tag]
  (merge http-fxs-commons
         {:method :delete
          :uri (str constants/server-url "/song/" song-id "/tags/" tag "?device-id=" device-id)}))

(defn set-song-as-favourite-http-fx [device-id song-id]
  (merge http-fxs-commons
         {:method :put
          :uri (str constants/server-url "/user/favourites/" song-id "?device-id=" device-id)}))

(defn unset-song-as-favourite-http-fx [device-id song-id]
  (merge http-fxs-commons
         {:method :delete
          :uri (str constants/server-url "/user/favourites/" song-id "?device-id=" device-id)}))

(defn search-songs-http-fx [device-id q]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/search?device-id=" device-id "&q=" q)}))

(defn song-by-id-http-fx [device-id song-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/" song-id "/get?device-id=" device-id)}))

(defn update-song-name-http-fx [device-id song-id new-song-name]
  (merge http-fxs-commons
         {:method :put
          :uri (str constants/server-url "/song/" song-id "/name?device-id=" device-id)
          :params {:new-song-name new-song-name}}))

(defn update-song-artist-http-fx [device-id song-id new-artist-name]
  (merge http-fxs-commons
         {:method :put
          :uri (str constants/server-url "/song/" song-id "/artist?device-id=" device-id)
          :params {:new-artist-name new-artist-name}}))

(defn update-song-album-http-fx [device-id song-id new-album-name]
  (merge http-fxs-commons
         {:method :put
          :uri (str constants/server-url "/song/" song-id "/album?device-id=" device-id)
          :params {:new-album-name new-album-name}}))

(defn search-artists-http-fx [device-id q]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/artist/search?device-id=" device-id "&q="q)}))

(defn search-album-http-fx [device-id q]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/album/search?device-id=" device-id "&q="q)}))

(defn track-song-play-http-fx [device-id song-id]
  (merge http-fxs-commons
         {:method :put
          :uri (str constants/server-url "/song/" song-id "/track-play?device-id=" device-id)}))
                                 
