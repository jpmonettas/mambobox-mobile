(ns mambobox-mobile.android.core
  (:require [reagent.core :as r :refer [atom create-class]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [mambobox-mobile.events]
            [mambobox-mobile.subs]
            [clojure.spec :as s]
            [mambobox-core.core-spec]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def list-view (r/adapt-react-class (.-ListView ReactNative)))
(def DataSource (-> ReactNative .-ListView .-DataSource))
(def view-pager (r/adapt-react-class (.-ViewPagerAndroid ReactNative))) 
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def video (r/adapt-react-class (.-default (js/require "react-native-video/Video"))))
(def icon (r/adapt-react-class (js/require "react-native-vector-icons/FontAwesome")))
(def scrollable-tab-view (r/adapt-react-class (js/require "react-native-scrollable-tab-view")))

(def pick (-> ReactNative .-NativeModules .-PickerModule .-pick))
(def upload-file (-> ReactNative .-NativeModules .-FileUpload .-upload))

(defn pick-and-upload []
  (.then (pick) (fn [real-path]
                  (.log js/console "Uploading selected file " real-path)
                  (upload-file (clj->js {:uploadUrl "http://192.168.1.8:1155/uploads"
                                         :method "POST"
                                         :headers {"Accept" "application/json"}
                                         :fields {"param1" "value1"}
                                         :files [{:filename "audiofile"
                                                  :filepath real-path}]})
                               (fn [err result]
                                 (.log js/console "Upload " err result))))))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
      (.alert (.-Alert ReactNative) title))

(defn build-list-view-datasource [rows-data]
  (let [ds (DataSource. #js {:rowHasChanged (constantly false)})]
    (.cloneWithRows ds rows-data)))

(defn format-duration [seconds]
  (str (quot seconds 60) ":" (mod seconds 60)))

(defn song [{:keys [song-name artist-name duration]}]
  [view {:style {:padding 10
                 :margin 2
                 :border-width 1
                 :border-color "rgba(0,0,0,0.1)"
                 :flex-direction :row
                 :justify-content "space-between"}}
   [view {:flex-direction :row}
    [icon {:name "music"
           :style {:padding 10
                   :margin 5
                   :background-color "rgba(0,0,0,0.1)"}
           :size 20}]
    [view 
     [text {:style {:font-weight :bold
                    :font-size 17}} song-name]
     [text {} (str artist-name " . " (format-duration duration))]]]
   [icon {:name "ellipsis-v"
          :style {:padding 10
                  :margin 5}
          :size 20}]])

(defn my-favorites []
  (let [favorites-songs (subscribe [:favorites-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @favorites-songs))
                 :renderRow (comp r/as-element song)}])))

(defn hot []
  [view {}
   [text {} "Hot"]])

(defn categories []
  [view {}
   [text {} "categories"]])

(defn app-root []
  [scrollable-tab-view 
   [view {:tab-label "Favorites"
          :style {:flex 1}}
    [my-favorites]]
   [view {:tab-label "Hot"} [hot]]
   [view {:tab-label "Categories"} [categories]]])

#_(defn pager []
  [view-pager {:init-page 0 :style {:height 300}} 
   [view {:style {:height 200 :background-color "#345678" }}
    ]
   [view {:style {:background-color "#FFFF45"}}
    [text {} "chauuuuuuuuusito"]]
   ])

#_(defn app-root []
  (let [greeting (subscribe [:get-greeting])
        player-status (subscribe [:player-status])]
    (fn []
      #_[pager]
      [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [text {} (str "holaaaaaaaaaa testtt : " (:playing @player-status) "]")]
       [video {:source {:uri "http://192.168.1.8:1122/Rehab.mp3"}
               :play-in-background true
               :play-when-inactive true
               :paused (:playing @player-status)}]
       [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} @greeting]
       [image {:source logo-img
               :style  {:width 80 :height 80 :margin-bottom 30}}]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press #(dispatch [:toggle-play])}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "stop/pause"]]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press pick-and-upload}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "upload file"]]])))



(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "MamboboxMobile" #(r/reactify-component app-root)))
