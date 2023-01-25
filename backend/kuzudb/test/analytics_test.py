import kuzudb.query_kuzu as query
import analytics.statistics as stat

""" get weighted collaboration"""
config = {  "from_year": 2010,
            "area_id" : "ai", 
            "area_type":  "a", 
            "region_id":"dach",
            "strict_boundary":True,
            "institution":False}

data = query.get_weighted_collab(config)
nodes = list(set(data["a"]) | set(data["b"]))
edges=[tuple(row.values) for _,row in data.iterrows()]

"""get graph statistics"""
G = stat.gen_graph(nodes, edges, weighted=True)
stat.get_statistics(G)

"""get top centralities"""
stat.get_centralities(G, weighted=True, top=5)

# G = stat.gen_graph([1, 2, 3, 4], [[2,1],[2,4],[1,3],[3,2],[1,4]])
# stat.get_statistics(G)