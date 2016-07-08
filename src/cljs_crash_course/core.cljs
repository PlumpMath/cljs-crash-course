(ns cljs-crash-course.core
  (:require [sablono.core])
  (:require-macros [sablono.core :refer [html]]))

(enable-console-print!)

;; We use 'defonce' so app state is not reinitialised on code reload.
(defonce app-state (atom {:todos []
                          :new-todo ""}))

;; Forward declaration
(declare build-ui)

(defn render-app
  "Render the UI on DOM node, according to state: UI=f(S)."
  [state node]
    (.render js/ReactDOM
             (build-ui state)
             node))

(defn set-todo
  "Set todo done value."
  [m i v]
  (assoc-in m [:todos i :done] v))

(defn add-todo
  "Add a todo item to the list and clear the input buffer."
  [m]
  (let [{:keys [new-todo]} m] ;; map destructuring
    (-> m ;; thread-first macro: result of expression becomes 1st param of next.
      (update :todos
              conj {:text new-todo
                    :done false})
      (assoc :new-todo ""))))

(defn todo-item
  "Render a todo item."
  [state index item]
  (html [:li
         {:key (str index)} ;; unique key for React to optimize  of collections
         [:input
          {:type "checkbox"
           :checked (:done item)
           :on-click (fn [_] ;; we don't need the event parameter
                       (swap! state
                              set-todo index (not (:done item))))}]
         [:span (when (:done item) ;; nil is valid inside sablono vectors
                  {:style {:text-decoration "line-through"}})
          (:text item)]]))

(defn todo-list
  "Render a list of todos."
  [state]
  (html [:ul (map-indexed
              (partial todo-item state) ;; map-indexed fn takes only 2 params
              (get @state :todos))]))

(defn input-form
  "Render input form."
  [state]
  (html [:div
         [:input
          {:type "text"
           :placeholder "todo (5 char min)"
           :value (:new-todo @state)
           :on-change (fn [e]
                        (swap! state
                               assoc
                               :new-todo
                               (.-value (.-target e))))}]
         [:input
          {:type "button"
           :value "add"
           :disabled (< (.-length (:new-todo @state)) 4)
           :on-click (fn [_] ;; React uses click events on checkbox, not change.
                       (swap! state add-todo))}]]))

(defn build-ui
  "Render the whole UI."
  [state]
  (html [:div
         (input-form state)
         (todo-list state)]))

(defn on-js-reload []
  "Touch app-state on code reload to force rendering."
  (swap! app-state update-in [:__figwheel_counter] inc))

;; We add a watcher to render the UI on state change.
(add-watch
 app-state
 :render
 (fn [_ atom _ _]
   ;; a watch fn takes keyword, atom, oldval, newval parms;
   ;; here we only want the atom ('_' is a throw-away wildcard).
   (render-app atom (.getElementById js/document "app"))))

;; Initial render
(render-app app-state (.getElementById js/document "app"))

(println "App started.")
