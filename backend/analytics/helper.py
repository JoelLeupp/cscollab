import kuzudb.query_kuzu as query
import networkx as nx
import numpy as np
import random

# http://konect.cc/networks/dblp-cite/

""" get weighted collaboration"""
config = {  "from_year": 2010,
            "area_id" : "ai", 
            "area_type":  "a", 
            "region_id":"dach",
            "strict_boundary":True,
            "institution":False}

data = query.get_weighted_collab(config)

"""create networkx graph """
G = nx.Graph() 

nodes = list(set(data["a"]) | set(data["b"]))
G.add_nodes_from(nodes)

edges=[tuple(row.values) for _,row in data.iterrows()]
G.add_weighted_edges_from(edges)

"""get nodes/edges"""
G.nodes()
G.number_of_nodes()
G.edges()
G.number_of_edges()

"""examine elements"""
G.degree("r/CarstenRother")
list(G.adj["61/5017"])

"""graph theoretical analytics"""
connected_components=list(nx.connected_components(G))
connected_components.sort(key=len,reverse=True)
connected_components_size = sorted(list(map(lambda x: len(x),connected_components)),reverse=True)

degrees = sorted(d for n, d in G.degree())
degree_avg = np.mean([d for _, d in G.degree()])

shortest_path_lengths = dict(nx.all_pairs_shortest_path_length(G))

nx.is_connected(G) #False
len(connected_components)
# This equivalent to `diameter = nx.diameter(G) but faster
diameter = max([max(j.values()) for (i,j) in nx.shortest_path_length(G)])

average_path_lengths = [
    np.mean(list(spl.values())) for spl in shortest_path_lengths.values()
]
# The average over all nodes
np.mean(average_path_lengths)

# sparse graph as desnity < 1
nx.density(G)

nx.number_connected_components(G)

degree_centrality= (sorted(nx.centrality.degree_centrality(G).items(), 
                    key=lambda item: item[1], reverse=True))

degree_centrality_weighted= (sorted(nx.centrality.degree_centrality(G).items(), 
                    key=lambda item: item[1], reverse=True))


betweenness_centrality=(sorted(nx.centrality.betweenness_centrality(G).items(),
                        key=lambda item: item[1], reverse=True))

closeness_centrality= (sorted(nx.centrality.closeness_centrality(G).items(), 
                    key=lambda item: item[1], reverse=True))

eigenvector_centrality=(sorted(nx.centrality.eigenvector_centrality(G).items(), 
                        key=lambda item: item[1], reverse=True))

eigenvector_centrality_weighted =(sorted(
                nx.centrality.eigenvector_centrality_numpy(G,weight='weight').items(), 
                        key=lambda item: item[1], reverse=True))


nx.clustering(G)
nx.average_clustering(G)

nx.has_bridges(G)
bridges = list(nx.bridges(G))
len(bridges)

local_bridges = list(nx.local_bridges(G, with_span=False))
len(local_bridges)

nx.degree_assortativity_coefficient(G)

community_idx = dict() 
counter = 0
for id, com in enumerate(nx.community.label_propagation_communities(G)):
    counter += 1
    for node in list(com):  # fill colors list with the particular color for the community nodes
        community_idx[node] = id
counter

# find communities given the numer of communities
nx.community.asyn_fluidc(G, 5, seed=0)

for com in nx.community.label_propagation_communities(G):
    print(len(com))

"""clear graph"""
G.clear()