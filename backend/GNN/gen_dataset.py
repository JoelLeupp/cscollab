import kuzudb.query_kuzu as query
import torch
import torch_geometric

# get connection to kuzu db
conn = query.conn

area_mapping = query.get_area_mapping()


dach_collab_inst = query.get_weighted_collab({ "from_year": 2015,
                                                "region_id":"dach",
                                                "strict_boundary":True,
                                                "institution":True
                                                })  


query.get_weighted_collab({"from_year": 2010, "institution":True})


