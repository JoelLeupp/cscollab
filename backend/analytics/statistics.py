import networkx as nx
import numpy as np
import pandas as pd

# https://networkx.org/nx-guides/content/exploratory_notebooks/facebook_notebook.html
# http://konect.cc/networks/dblp-cite/

def gen_graph(nodes, edges, weighted=False):
    """generate networkx graph object"""   
    G = nx.Graph() 
    G.add_nodes_from(nodes)
    if weighted:
        G.add_weighted_edges_from(edges)
    else:
        G.add_edges_from(edges)
    return G


def get_statistics(G):
    """core graph theoretical statistics"""
    degrees = sorted(d for n, d in G.degree())
    connected_components=list(nx.connected_components(G))
    connected_components_size = sorted(list(map(lambda x: len(x),connected_components)),reverse=True)
    # shortest_path_lengths = dict(nx.all_pairs_shortest_path_length(G))
    # average_path_lengths = np.mean([np.mean(list(spl.values())) for spl in shortest_path_lengths.values()])
    
    statistics = {
        "nodes": G.number_of_nodes(),
        "edges": G.number_of_edges(),
        "loops": nx.number_of_selfloops(G),
        "triangle_count" : int(sum(nx.triangles(G).values()) / 3),
        "max_degree": max(degrees),
        "average_degree": np.mean(degrees),
        "is_connected": nx.is_connected(G),
        "number_connected_components": nx.number_connected_components(G),
        "largest_connected_component": max(connected_components_size),
        "density": nx.density(G),
        "clustering_coefficient": nx.average_clustering(G),
        "degree_assortativity_coefficient": nx.degree_assortativity_coefficient(G)
        }
    return statistics

def __tuple_to_dict(list_of_tuples):
    d= dict(zip(
        list(map(lambda x: x[0],list_of_tuples)),
        list(map(lambda x: x[1],list_of_tuples))
        ))
    return d
    
def get_centralities(G, weighted=False, top=None):
    
    """ filter based on the nodes with the highest scores"""
    n = G.number_of_nodes()
    if (top is None) or top >=n:
        top = n-1
    
    """importance score based on the number of links held by each node"""
    degree_centrality= (sorted(nx.centrality.degree_centrality(G).items(), 
                    key=lambda item: item[1], reverse=True))[:top]

    # """number of times the node lies on the shortest path between other nodes"""
    # betweenness_centrality=(sorted(nx.centrality.betweenness_centrality(G).items(),
    #                         key=lambda item: item[1], reverse=True))[:top]

    # """score based on closeness to all other nodes"""
    # closeness_centrality= (sorted(nx.centrality.closeness_centrality(G).items(), 
    #                     key=lambda item: item[1], reverse=True))[:top]

    """how connected a node is to other important nodes"""
    if weighted:
        eigenvector_centrality =(sorted(
                    nx.centrality.eigenvector_centrality_numpy(G,weight='weight').items(), 
                            key=lambda item: item[1], reverse=True))[:top]
    else:
        eigenvector_centrality=(sorted(nx.centrality.eigenvector_centrality(G).items(), 
                                key=lambda item: item[1], reverse=True))[:top]

    centralities = {"degree_centrality":__tuple_to_dict(degree_centrality),
                    # "betweenness_centrality":__tuple_to_dict(betweenness_centrality),
                    # "closeness_centrality":__tuple_to_dict(closeness_centrality),
                    "eigenvector_centrality":__tuple_to_dict(eigenvector_centrality)}   
    return centralities