(ns app.cscollab.view.guide.guide
  (:require
   [app.components.layout :as acl]
   [app.components.colors :as colors]
   [app.router :as router]))

(defn guide-view []
  #_[acl/section {:color :white :style {:font-size 20}}]
  [:div {:style {:width "100%" :background-color :white :font-size 20}}
   [:div {:style {:padding 20}}
    ;; Overview
    [:h2 {:style {:margin 0 :width "100%"}} "Overview"]
    [:p {:style {:width "100%"}}
     "This application serves as a visualizer, analyzer and explorer of the collaboration network of computer science
            conference papers from the year 2015-2022 with focus on the field of AI. The collaboration network was created by combining data from"
     [:b [:a {:href "https://dblp.org/"} " dblp "]]
     "a computer science bibliography database with data from " [:b [:a {:href "https://csrankings.org/"} "csrankings"]]

     " which provides detailed information about authors and their affiliation to institutions and countries. The collaboration network 
      is therefor based only on authors that are included in the Computer Science Rankings. Since every author is affiliated with an institution
      the collaborations between authors can be aggregated by their institution. In the collaboration network the nodes represent therefor
      either an author or an instituion and edges represent collaborations."]
    [:p "The publications are categorized by the research areas (4 main areas: AI, Systems, Theory and Interdisciplinary Areas and 23 sub areas)
      and for every research area only the papers from the leading conferences are considered. The conferences where selected based on the impact score
      from" [:b [:a {:href "https://research.com/conference-rankings/computer-science"} " research.com "]]
     "and the selection of conferences from CSRankings.                                                  
      The network includes a total of 76'546 publications from 127 Conferences, 148'379 individual collaborations between authors, 14'555 Authors
      and 597 Institutions."]
    [:p "The application provides a various visualizations, analytics and statistics of the collaboration network among others a geographical 
         visualization of the collaboration network in the form of an interactive map and a graph representation
         where the nodes are grouped by reseach areas which is done by using Graph Convolutional Network Models. Further, several explorers are provided to
         make the data transparent and accessible and allow a detailed inspecation of the data. A more detailed documentation and the full code of the dataset generation, the backend and the frontend
         can be viewed at the public GitHub repository " [:b [:a {:href "https://github.com/JoelLeupp/cscollab"} "cscollab"]] "."]
    ;; Visualization
    [:a {:href (router/href :home)} [:h2 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Visualization"]]
    ;; Filters
    [:h3 {:style {:margin 0 :width "100%"}} "Filters, Configuration and Interactions"]
    [:p "On top of the page there is a panel where one can filter, configer and interact with the collaboration network. 
         To apply the filters and load the new collaboration data the refresh button in the top right corner 
         of the visualization views has to be clicked."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/config.PNG" :width "100%"}]]
    [:p "The graph filter panel allows to filter the collaboration network based on:"
     [:ul
      [:li "the publication year (2005-2022)"]
      [:li "research area/sub area of the publication"]
      [:li "region/country of the authors or affiliated institutions"]
      [:li "An option to set a strict country/region restriction where only collaborations within the selected regions are considered"]]
     "In the graph configuaration and interaction panel the following options are available:"
     [:ul
      [:li "chose if one is interested in the author collaboration or the collaboration between institutions"]
      [:li "one can select a node and click 'show' which will highlight the node and zoom to the node position in the current visualization"]
      [:li "one can choose to color the nodes in the current graph visualization where one has the following options: no coloring, color by top area,
            color by top sub area, color by degree centrality or color by eigenvector centrality"]]]
    ;; Views
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Geographical Visualization, Interactive Map"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/map.PNG" :width "100%"}]]
    [:p "The selected collaboration network can be viewed as a geographical visualisation in form of an interactive map. 
         One can zoom and pan the map and fully explore the network (also full screen option available). 
         If you click on an edge or node (institution or author icon depending on the chosen network) all nodes or edges connected 
         to that node/edge will be highlighted in green and an information box on the right will open which shows 
         information of the node and multiple tabs with different visualizations of network data of that node/edge."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/map_interaction.PNG" :width "100%"}]]
    [:p "Available visulaizations of nodes are:"
     [:ul
      [:li "Publication plot: show in how many publications of each research area that node collaborated in"]
      [:li "Auhtor list (only if the node is an institution): a list of all authors affiliated with that institution ranked by their publication count"]
      [:li "Institution plot: shows with which institutions that node had collaborated and how my publications per institution"]
      [:li "Country plot: shows with which countries that node had collaborated and how my publications per countries"]
      [:li "Year plot: shows in how many publicaitons that node collaborated in each year"]
      [:li "Author collaboration plot: shows with which authors that node had collaborated and how my publications per authors"]]]
    ;; Graph
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Graph Representation"]
    [:p "The collaboration network is visualized as a graph where interactions are the same as with the map component. 
         The graph can be viewed in full screen and one can zoom and pan the graph and click on edges/nodes and get exactly 
         the same information box and charts on the right as in the geographical map."]
    [:p "The determin the position of the nodes a graph convolutional network model (GCN) was used that was trained based on a node classification task. 
         The task was to classify nodes (institutions or authors) by the research area or sub area where they had to most publicaitons in. 
         This results in 4 models: 2 for the author collaboration network where one predicts nodes on the top research area and the other 
         on the top sub area and 2 models for the institutional network where one predicts nodes on the top research area and the other 
         on the top sub area. The position of the nodes can be determined by using the last hidden convolutional output layer and apply 
         a dimensionality reduction algorithm on it project the results on a 2 dimensioanl space. This allows us the position the 
         nodes based on the results of the GCN model and have the nodes grouped by their top research area/sub area."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/graph.PNG" :width "100%"}]]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/graph_area.PNG" :width "100%"}]]
    ]])