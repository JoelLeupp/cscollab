import kuzudb.query_kuzu as query
import networkx as nx


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
G.degree("61/5017")
list(G.adj["61/5017"])

"""graph theoretical analytics"""
connected_components=list(nx.connected_components(G))
sorted(list(map(lambda x: len(x),connected_components)))

degrees = sorted(d for n, d in G.degree())


"""clear graph"""
G.clear()