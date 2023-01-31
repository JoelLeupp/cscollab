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


