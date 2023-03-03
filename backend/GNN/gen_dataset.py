import kuzudb.query_kuzu as query
import collections
import torch
import torch_geometric
import torch_geometric.transforms as T
from sklearn.preprocessing import OneHotEncoder
from sklearn.preprocessing import LabelEncoder
import numpy as np
import functools
import json
import pandas as pd

""" mapping of sub areas to areas"""
area_mapping = query.get_area_mapping()
subarea_mapping = dict(zip(area_mapping["sub-area-id"],area_mapping["area-id"]))

"""one hot encoding"""
onehot_encoder = OneHotEncoder(sparse_output=False)

area_ids = list(area_mapping["area-id"].unique())
n_area = len(area_ids)
onehot_encoded_areas = onehot_encoder.fit_transform(np.arange(n_area).reshape(n_area,1))
tensor_list_ohe_areas = list(map(lambda x: torch.tensor(x, dtype=torch.float), onehot_encoded_areas))
area_ohe_mapping = dict(zip(area_ids,tensor_list_ohe_areas))
area_binary_mapping = dict(zip(area_ids,[[0,0],[1,1],[1,0],[0,1]]))


sub_area_ids = list(area_mapping["sub-area-id"].unique())
n_sub_area = len(sub_area_ids)
onehot_encoded_sub_areas = onehot_encoder.fit_transform(np.arange(n_sub_area).reshape(n_sub_area,1))
# tensor_list_ohe_sub_areas = list(map(lambda x: torch.tensor(x, dtype=torch.float), onehot_encoded_sub_areas))
sub_area_ohe_mapping = dict(zip(sub_area_ids,onehot_encoded_sub_areas))

"""mapping of author to institution"""
csauthors_all = query.get_csauthors_no_cache()
author_inst_map = dict(zip(csauthors_all["pid"],csauthors_all["institution"]))

""" count the frequency of sub/areas for a node"""
# def area_frequency_counter(collab, node):
#     collab_node = collab[(collab["a"]==node) | (collab["b"]==node)]
#     area_counter = dict(collections.Counter(collab_node["rec_area"]))
#     top_area = max(area_counter, key=area_counter.get)
#     sub_area_counter = dict(collections.Counter(collab_node["rec_sub_area"]))
#     top_sub_area = max(sub_area_counter, key=sub_area_counter.get)
#     freq= {"area_freq":area_counter,
#             "top_area":top_area,
#             "sub_area_freq":sub_area_counter,
#             "top_sub_area":top_sub_area}
#     return freq

def get_freq(a):
    return json.dumps(dict(collections.Counter(a)))

def merge_freq(a,b):
    freq_a = json.loads(a) if pd.notnull(a) else None
    freq_b = json.loads(b) if pd.notnull(b) else None
    inp = [freq_a,freq_b]
    merged_freq = dict(sum((collections.Counter(y) for y in inp), collections.Counter()))
    return json.dumps(merged_freq)

def freq_summary(freq_counter):
    top = max(freq_counter, key=freq_counter.get)
    freq= { "freq":freq_counter,
            "top":top}
    return freq

def frequeny_counter(collab,use_sub_areas=True):
  
    area_coll = "rec_sub_area" if use_sub_areas else "rec_area"

    area_freq_a=collab.groupby(["a"])[area_coll].apply(get_freq)
    area_freq_b=collab.groupby(["b"])[area_coll].apply(get_freq)
    freq_merged = pd.merge(area_freq_a,area_freq_b,right_index = True, left_index = True,how="outer")
    freq_merged.columns = ["a","b"]
    freq_merged["freq"]=list(map(lambda a,b: merge_freq(a,b) ,freq_merged["a"],freq_merged["b"]))
    frequency_map = dict(zip(freq_merged.index,
                            list(map(lambda x: freq_summary(json.loads(x)),freq_merged["freq"]))))
    return frequency_map

def get_collab_data(config):
    """get collaboration"""
    collab_flat = query.get_flat_collaboration(ignore_area=False,use_cache=False)
    collab = query.filter_collab(collab_flat,config)
    institution = config.get("institution")

    collab["rec_area"]=list(map(lambda x: subarea_mapping[x], collab["rec_sub_area"]))  
        
    node_a = "a_inst" if institution else "a_pid"
    node_b = "b_inst" if institution else "b_pid"
    collab["a"] = collab[node_a]
    collab["b"] = collab[node_b]

    """map each institution or author to an int based on the positional index"""
    nodes = list(set(collab["a"]).union(set(collab["b"])))
    node_idx = list(range(len(nodes)))
    node_idx_mapping = dict(zip(nodes, node_idx))
    node_mapping = [node_idx, nodes] #list(map(list,zip(node_idx,nodes)))

    frequency_map = dict(zip(node_idx, list(map(lambda n: area_frequency_counter(collab, n), nodes))))

    collab_weighted = query.weighted_collab(collab,institution=institution)
    collab_weighted["a"] = list(map(lambda n: node_idx_mapping[n],collab_weighted["a"]))
    collab_weighted["b"] = list(map(lambda n: node_idx_mapping[n],collab_weighted["b"]))
    
    adjacency_list = list(map(list,zip(collab_weighted["a"], collab_weighted["b"])))
    weights = collab_weighted["weight"].values

    data = {"node_mapping":node_mapping,
            "nodes": node_idx,
            "edges":adjacency_list,
            "weights":weights,
            "freq": frequency_map}
    return data

def prepare_data(collab, institution=False):

    collab["rec_area"]=list(map(lambda x: subarea_mapping[x], collab["rec_sub_area"]))  
        
    node_a = "a_inst" if institution else "a_pid"
    node_b = "b_inst" if institution else "b_pid"
    collab["a"] = collab[node_a]
    collab["b"] = collab[node_b]

    """map each institution or author to an int based on the positional index"""
    nodes = list(set(collab["a"]).union(set(collab["b"])))
    node_idx = list(range(len(nodes)))
    node_idx_mapping = dict(zip(nodes, node_idx))
    node_mapping = [node_idx, nodes] #list(map(list,zip(node_idx,nodes)))

    frequency_map = frequeny_counter(collab)
    frequency_map_idx = {node_idx_mapping[k]:v  for (k,v) in frequency_map.items()}

    collab_weighted = query.weighted_collab(collab,institution=institution)
    collab_weighted["a"] = list(map(lambda n: node_idx_mapping[n],collab_weighted["a"]))
    collab_weighted["b"] = list(map(lambda n: node_idx_mapping[n],collab_weighted["b"]))

    adjacency_list = list(map(list,zip(collab_weighted["a"], collab_weighted["b"])))
    weights = collab_weighted["weight"].values

    data = {"node_mapping":node_mapping,
            "nodes": node_idx,
            "edges":adjacency_list,
            "weights":weights,
            "freq": frequency_map_idx}      
    return data

def get_top_area(freq):
    area_count = dict.fromkeys(area_ids,0)
    for k,v in freq.items():
        a = subarea_mapping[k]
        area_count[a]+=v
    top_area = max(area_count, key=area_count.get)
    return top_area

"""get target label as the area with the most records published"""
def get_y(nodes, freq, use_sub_areas=True):

    ids = sub_area_ids if use_sub_areas else area_ids
    if use_sub_areas:
        top = list(map(lambda x: freq[x]["top"], nodes))
    else:
        top = list(map(lambda x: get_top_area(freq[x]["freq"]), nodes))
    y = torch.tensor(list(map(lambda x: ids.index(x),top)),dtype=torch.long)
    return y

def sub_area_frequency(freq,n):
    sfreq = freq[n]["freq"]
    """sum up one hot endocings of the subareas by their frequency"""
    freq_array = functools.reduce(lambda x, key: x + sfreq[key] * sub_area_ohe_mapping[key], sfreq, np.zeros(n_sub_area))
    """get frequency as percentage such that it sums up to 1"""
    freq_array_p = freq_array/sum(sfreq.values())
    return freq_array_p 

"""the percentage of published subareas"""
def get_x(nodes,freq):
    x =  torch.tensor(np.array(list(map(lambda node: sub_area_frequency(freq,node) ,nodes))), dtype=torch.float)
    return x

def gen_torch_data(nodes,edges, freq,node_mapping ,weights=None,use_sub_areas=False):
    
    edge_index = torch.tensor(edges, dtype=torch.long).t().contiguous()
    
    y = get_y(nodes,freq,use_sub_areas)
    
    x=get_x(nodes, freq)
    
    if weights is not None:
        weights_normalized = weights / sum(weights)
        weights = torch.tensor(weights_normalized,dtype=torch.float)
        
    """create torch data object without weights"""
    data = torch_geometric.data.Data(x=x, y=y, edge_index=edge_index, num_nodes=len(nodes),edge_weight=weights,node_mapping=node_mapping)
    data = T.ToUndirected()(data) # the collaboration network is undirected
    data = T.AddSelfLoops()(data) # by adding self-loops, we ensure that aggregated messages from neighbors 
    data = T.NormalizeFeatures()(data) # features will sum up to 1
    """ define train test split"""
    transform = torch_geometric.transforms.RandomNodeSplit(split='train_rest', num_val=0.3, num_test=0)
    transform(data)
    return data

def collab_to_torch(collab_data, weighted=True,use_sub_areas=True):
    nodes = collab_data["nodes"]
    edges = collab_data["edges"]
    weights = collab_data["weights"] if weighted else None
    freq = collab_data["freq"]
    node_mapping=collab_data["node_mapping"]
    data = gen_torch_data(nodes, edges, freq,node_mapping, weights=weights,use_sub_areas=use_sub_areas)
    return data


"""wrapper to return torch data object from config"""
def get_torch_data(config, use_sub_areas):
    
    collab_flat = query.get_flat_collaboration(ignore_area=False,use_cache=False)
    collab_filtered = query.filter_collab(collab_flat,config)

    institution = config.get("institution")

    collab_data = prepare_data(collab_filtered,institution)
    data_area = collab_to_torch(collab_data,weighted=True,use_sub_areas=use_sub_areas)
    return data_area

# use_sub_areas = False
# config = { "from_year": 2005,
#             "region_ids":["wd"],
#             "strict_boundary":True,
#             "institution":False}
# collab_flat = query.get_flat_collaboration(ignore_area=False,use_cache=False)
# collab_filtered = query.filter_collab(collab_flat,config)
# institution = config.get("institution")

# collab_data = prepare_data(collab_filtered,institution)
# data = collab_to_torch(collab_data,weighted=True,use_sub_areas=use_sub_areas)



