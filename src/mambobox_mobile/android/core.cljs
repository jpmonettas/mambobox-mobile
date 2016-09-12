(ns mambobox-mobile.android.core
  (:require [reagent.core :as r :refer [atom create-class]]
            [reagent.ratom :refer [reaction]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [mambobox-mobile.events]
            [mambobox-mobile.subs]
            [clojure.spec :as s]
            [mambobox-core.core-spec]
            [devtools.core :as dt]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def slider (r/adapt-react-class (.-Slider ReactNative)))
(def list-view (r/adapt-react-class (.-ListView ReactNative)))
(def tool-bar (r/adapt-react-class (.-ToolbarAndroid ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def DataSource (-> ReactNative .-ListView .-DataSource))
(def view-pager (r/adapt-react-class (.-ViewPagerAndroid ReactNative))) 
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))
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

(defn song [{:keys [song-name artist-name duration] :as song}]
  [touchable-opacity {:on-press #(dispatch [:play-song song])}
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
           :size 20}]]])

(defn my-favorites-tab []
  (let [favorites-songs (subscribe [:favorites-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @favorites-songs))
                 :renderRow (comp r/as-element song)}])))

(defn hot-tab []
  (let [hot-songs (subscribe [:hot-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @hot-songs))
                 :renderRow (comp r/as-element song)}])))

(def tags [["chacha" "#ff0000"]
           ["mambo" "#9303a7"]
           ["latin-jazz" "#993366"]
           ["guaracha" "#64a8d1"]
           ["salsa dura" "#2219b2"]
           ["romantica" "#cb0077"]
           ["bolero" "#e5399e"]
           ["pachanga" "#999900"]
           ["boogaloo" "#d9534f"]
           ["son" "#ff7800"]
           ["montuno" "#ff9a40"]
           ["songo" "#ffa700"]
           ["danzon" "#ffbd40"]
           ["rumba" "#138900"]
           ["guaguanco" "#389e28"]
           ["yambu" "#1dd300"]
           ["columbia" "#52e93a"]
           ["afro" "#a64b00"]])

(defn tags-line [[t1-text t1-bg t1-fg]
                 [t2-text t2-bg t2-fg]]
  (let [tag-style {:padding 20
                   :text-align "center"
                   :margin 10
                   :flex 0.5
                   :font-weight :bold
                   :font-size 15
                   :color "white"}]
   [view {:flex-direction :row}
    [text {:style (merge tag-style {:background-color t1-bg})} t1-text]
    [text {:style (merge tag-style {:background-color t2-bg})} t2-text]]))

(defn tags-tab []
  [scroll-view 
   (for [[t1 t2] (partition-all 2 tags)]
     ^{:key (first t1)} [tags-line t1 t2])])

(defn header []
  [view {:style {:background-color "#9303a7"}}
   [tool-bar {:title "MamboBox"
              :title-color :white
              :style {:height 40}
              :actions [{:title "Subir musica"} {:title "Preferencias"}]}]
   [text-input {:placeholder "Buscar musica..."
                :placeholder-text-color :white
                :underline-color-android :white}]])

(def video-player-instance (cljs.core/atom))

(defn video-player-wrapper [url paused]
  (create-class
   {:component-did-mount (fn [this]
                           )
    :reagent-render (fn []
                      [video {:source {:uri url}
                              :play-in-background true
                              :play-when-inactive true
                              :paused paused}])}))
(defn player []
  (let [player-status (subscribe [:player-status])]
    (fn []
      (let [ps @player-status
            {:keys [song-name artist-name url]} (:playing-song ps)]
       [view {:style {:border-width 1
                      :padding-top 10
                      :padding-bottom 10}}
        [view {:style {:flex-direction :row
                       :justify-content :space-between
                       :padding-bottom 10}}
         [view {:flex-direction :row}
          [icon {:name "headphones"
                 :style {:padding 10
                         :margin 5
                         :background-color "rgba(0,0,0,0.1)"}
                 :size 20}]
          [view {}
           [text {:style {:font-weight :bold
                          :font-size 17}}
            song-name]
           [text {} artist-name]]]
         [view {:style {:margin 10 :flex-direction :row}}
          [icon {:name "step-backward" :size 18 :style {:margin 10}}]
          [touchable-opacity {:on-press #(dispatch [:toggle-play])}
           [icon {:name (if (:paused ps) "play" "pause")
                  :size 25
                  :style {:margin 5}}]]
          [icon {:name "step-forward" :size 18 :style {:margin 10}}]]]
        [slider]
        [video-player-wrapper url (:paused ps)]]))))

(defn app-root []
  (let [selected-tab (subscribe [:selected-tab])]
    (fn []
      [view {:style {:flex 1}}
       [header]
       [scrollable-tab-view {:initial-page @selected-tab
                             :on-change-tab #(dispatch [:change-tab (.-i %)])
                             :tab-bar-background-color "#9303a7"
                             :tab-bar-active-text-color :white
                             :tab-bar-inactive-text-color :white
                             :tab-bar-underline-style {:background-color "white"}}
        [view {:tab-label "Favorites"
               :style {:flex 1}}
         [my-favorites-tab]]
        [view {:tab-label "Hot"
               :style {:flex 1}} [hot-tab]]
        [view {:tab-label "Explore"
               :style {:flex 1}} [tags-tab]]]
       [player]])))

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
