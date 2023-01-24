import networkx as nx
import numpy as np

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
    shortest_path_lengths = dict(nx.all_pairs_shortest_path_length(G))
    average_path_lengths = np.mean([np.mean(list(spl.values())) for spl in shortest_path_lengths.values()])
    
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
        "average_shortest_path": average_path_lengths,
        "density": nx.density(G),
        "clustering_coefficient": nx.average_clustering(G),
        "degree_assortativity_coefficient": nx.degree_assortativity_coefficient(G)
        }
    return statistics
    