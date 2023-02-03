# Interactive Visualization of Scientific Collaboration Networks based on Graph Neural Networks


The goal of this project is to visualize the collaboration between authors and institutions in computer science conferences with focus on the field of AI in an interactive geographical map and graph view. For this the data from dblp a computer science bibliography is used in combination with data from csrankings which provides detailed information about authors and their affiliation to institutions and countries. 

The project is split into 3 main parts each with it's own detailed readme: 
* data generation 
* Backend  
* Frontend

## Data Generation 

Here the graph datasets are generated. The data from dblp and csrankings (and web) gets extracted, cleaned, combined and restructured. The generated graph structured data is then loaded into the property graph database kuzu db and several queries and aggregations from the db are provided to access the data.

## Backend

The backend is implemented as a dokerized flask server and provides APIs to query and aggregate data from the kuzu db. Further a graph analytics engin is implemented in the bakckend to calculate core graph statistics and analysis. A graph neural network engin will also be implemented to make node predictions on the collaboration network.

run backend:
```bash
make docker-build  && make docker-run 
```

## Frontend

The frontend is built with the ClojureScript framework re-frame and uses Material UI for user interface components. 

To run the frontend start a server in the output directory:

```bash
python3 -m http.server 8080
```
