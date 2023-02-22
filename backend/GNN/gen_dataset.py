import kuzudb.query_kuzu as query
import collections
import torch
import torch_geometric

""" mapping of sub areas to areas"""
area_mapping = query.get_area_mapping()
subarea_mapping = dict(zip(area_mapping["sub-area-id"],area_mapping["area-id"]))
area_ids = area_mapping["area-id"].unique()
sub_area_ids = area_ids = area_mapping["sub-area-id"].unique()

"""mapping of author to institution"""
csauthors_all = query.get_csauthors()
author_inst_map = dict(zip(csauthors_all["pid"],csauthors_all["institution"]))

""" count the frequency of sub/areas for a node"""
def area_frequency_counter(collab, node):
    collab_node = collab[(collab["a"]==node) | (collab["b"]==node)]
    area_counter = dict(collections.Counter(collab_node["rec_area"]))
    top_area = max(area_counter, key=area_counter.get)
    sub_area_counter = dict(collections.Counter(collab_node["rec_sub_area"]))
    top_sub_area = max(sub_area_counter, key=sub_area_counter.get)
    freq= {"area_freq":area_counter,
            "top_area":top_area,
            "sub_area_freq":sub_area_counter,
            "top_sub_area":top_sub_area}
    return freq


def get_collab_data(config):
    
    """get collaboration"""
    collab = query.get_collaboration(config)  
    collab["rec_area"]=list(map(lambda x: subarea_mapping[x], collab["rec_sub_area"]))  
        
    if config.get("institution",False):
        collab["a"] = list(map(lambda x: author_inst_map[x], collab["a"].values))
        collab["b"] = list(map(lambda x: author_inst_map[x], collab["b"].values))


    """map each institution or author to an int based on the positional index"""
    nodes = list(set(collab["a"]).union(set(collab["b"])))
    node_idx = list(range(len(nodes)))
    node_idx_mapping = dict(zip(nodes, node_idx))

    frequency_map = dict(zip(node_idx, list(map(lambda n: area_frequency_counter(collab, n), nodes))))

    collab_weighted = query.get_weighted_collab(config)
    collab_weighted["a"] = list(map(lambda n: node_idx_mapping[n],collab_weighted["a"]))
    collab_weighted["b"] = list(map(lambda n: node_idx_mapping[n],collab_weighted["b"]))
    
    edge_tuples = list(map(list,zip(collab_weighted["a"], collab_weighted["b"])))
    weights = collab_weighted["weight"]

    data = {"nodes": node_idx,
            "edges":edge_tuples,
            "weights":weights,
            "freq": frequency_map}
    return data


dach_inst_config = { "from_year": 2015,
                    "region_id":"dach",
                    "strict_boundary":True,
                    "institution":True}

dach_inst_data = get_collab_data(dach_inst_config)
