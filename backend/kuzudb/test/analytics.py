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