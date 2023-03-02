import kuzudb.query_kuzu as query
import collections
import torch
import torch_geometric
import torch_geometric.transforms as T
from sklearn.preprocessing import OneHotEncoder
from sklearn.preprocessing import LabelEncoder
import numpy as np
import functools

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
print(area_ohe_mapping)

sub_area_ids = list(area_mapping["sub-area-id"].unique())
n_sub_area = len(sub_area_ids)
onehot_encoded_sub_areas = onehot_encoder.fit_transform(np.arange(n_sub_area).reshape(n_sub_area,1))
# tensor_list_ohe_sub_areas = list(map(lambda x: torch.tensor(x, dtype=torch.float), onehot_encoded_sub_areas))
sub_area_ohe_mapping = dict(zip(sub_area_ids,onehot_encoded_sub_areas))

"""mapping of author to institution"""
csauthors_all = query.get_csauthors_no_cache()
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

"""get target label as the area with the most records published"""
def get_y(nodes, freq, method=0, use_sub_areas=False):
    if use_sub_areas:
        top = list(map(lambda x: freq[x]["top_sub_area"], nodes))
        ids = sub_area_ids
        ohe = sub_area_ohe_mapping
    else:
        top = list(map(lambda x: freq[x]["top_area"], nodes))
        ids = area_ids
        ohe = area_ohe_mapping
    
    
    """get target label as torch tensor"""
    if method==0:
        """binary represnetation of y"""
        y = torch.tensor(list(map(lambda x: list(area_binary_mapping[x]),top)),dtype=torch.float)
    elif method ==1: 
        """one hot endcoding representation of y"""
        y = torch.tensor(list(map(lambda x: list(ohe[x]),top)),dtype=torch.float)
    elif method == 2:
        """int representation of y"""
        y = torch.tensor(list(map(lambda x: ids.index(x),top)),dtype=torch.long)
    return y

def sub_area_frequency(freq,n):
    sfreq = freq[n]["sub_area_freq"]
    """sum up one hot endocings of the subareas by their frequency"""
    freq_array = functools.reduce(lambda x, key: x + sfreq[key] * sub_area_ohe_mapping[key], sfreq, np.zeros(n_sub_area))
    """get frequency as percentage such that it sums up to 1"""
    freq_array_p = freq_array/sum(sfreq.values())
    return freq_array_p 

"""the percentage of published subareas"""
def get_x(nodes,freq):
    x =  torch.tensor(np.array(list(map(lambda node: sub_area_frequency(freq,node) ,nodes))), dtype=torch.float)
    return x

def gen_torch_data(nodes,edges, freq,node_mapping, use_x=True ,weights=None, y_method=0,only_features=False,use_sub_areas=False):
    
    edge_index = torch.tensor(edges, dtype=torch.long).t().contiguous()
    
    y = get_y(nodes,freq,y_method,use_sub_areas)
    
    ohe = torch.eye(len(nodes))
    if use_x:
        if only_features:
            x=get_x(nodes, freq)
        else:
            x = torch.cat((get_x(nodes, freq), ohe), dim=1)
       
    else:
        x = ohe
    
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

def collab_to_torch(config, weighted=False, use_x =True, y_method=0,only_features=False,use_sub_areas=False):
    collab_data = get_collab_data(config)
    nodes = collab_data["nodes"]
    edges = collab_data["edges"]
    weights = collab_data["weights"] if weighted else None
    freq = collab_data["freq"]
    node_mapping=collab_data["node_mapping"]
    data = gen_torch_data(nodes, edges, freq,node_mapping, use_x=use_x, weights=weights, y_method=y_method,only_features=only_features,use_sub_areas=use_sub_areas)
    return data

# config = { "from_year": 2015,
#             "region_id":"dach",
#             "strict_boundary":True,
#             "institution":True}
# data = collab_to_torch(config)

# # d= get_collab_data(config)
# nodes = d["nodes"]
# edges = d["edges"]
# freq = d["freq"]
# data.x[0]

# freq[0]
# data.x[0]
# # print()
# print(data)
# print('===========================================================================================================')
# # Gather some statistics about the graph.
# print(f'Number of nodes: {data.num_nodes}')
# print(f'Number of edges: {data.num_edges}')
# print(f'Number of features: {data.num_features}')
# print(f'Number of training nodes: {data.train_mask.sum()}')
# print(f'Has isolated nodes: {data.has_isolated_nodes()}')
# print(f'Has self-loops: {data.has_self_loops()}')
# print(f'Is undirected: {data.is_undirected()}')


# config = { "from_year": 2015,
#                     "region_id":"dach",
#                     "strict_boundary":True,
#                     "institution":True}

# collab_data = get_collab_data(config)
# nodes = collab_data["nodes"]
# edges = collab_data["edges"]
# weights = collab_data["weights"]
# freq = collab_data["freq"]
# data = gen_torch_data(nodes, edges, freq, weights=weights)
