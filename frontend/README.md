# Frontend

## Tooling

### Prerequisites
For the development of the frontend [clojure](https://clojure.org/guides/getting_started) (requires Java) is used in combination with [Node.js](https://nodejs.org/en/) an the NPM package manager.

The required software could be installed with the following commands:

```bash
apt update
apt install default-jre #install java
brew install clojure/tools/clojure #install clojure
apt install nodejs 
apt install npm 
```

### Packages

The application is built with the [ClojureScript](https://clojurescript.org/index) framework [re-frame](https://day8.github.io/re-frame/re-frame/), which leverages [React](https://reactjs.org/) and [Reagent](https://reagent-project.github.io/). For the UI design and components google's material UI library [mui](https://mui.com/) is used. The required cljs packages are defined under *deps.edn and* the packages are downloaded automatically when the project is compiled with [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html). The javascipt packages are defined under *package-lock.json* and can be installed with "npm i --save" 

## File Ovierview
```
├── src/app
│   ├── common
│   |   ├── **/*.cljs     # reagent components and events
│   ├── components
│   |   ├── **/*.cljs     # cljs wrappers for material ui components
│   ├── cscollab
│   |   ├── data.cljs     # backend data manipulation 
│   |   ├── nav.cljs      # app navigation
│   |   ├── views.cljs    # all app views
│   ├── core.cljs         # init application and events
│   ├── db.cljs           # define application database and its events
│   ├── router.cljs       # define router and its events
│   ├── util.cljs         # general utility functions
│   ├── views.cljs        # define the main app view and routes
├── public
│   ├── css
│   ├── data              # static data files
│   ├── js                # generated compiled js files
│   ├── index.html
├── target
│   ├── index.js          # generated file by shadow-cljs
├── output                # directory of the project release
├── deps.edn              # cljs package requirements
├── shadow-cljs.edn       # shadow-cljs project configuration 
├── node_modules          # js packages
├── package.json          # js package requirement
└── .gitignore
```

