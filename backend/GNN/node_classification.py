import torch
from matplotlib import pyplot as plt
import matplotlib as mpl
import seaborn as sns
import pandas as pd
from sklearn.decomposition import TruncatedSVD
import kuzudb.query_kuzu as query
import collections
import torch
import torch_geometric
import torch_geometric.transforms as T
from sklearn.preprocessing import OneHotEncoder
from sklearn.preprocessing import LabelEncoder
import numpy as np
import json
import functools
from sklearn.manifold import TSNE
from MulticoreTSNE import MulticoreTSNE
from torch.nn import Linear
import torch.nn.functional as F
import GNN.gen_dataset as dataset 

# import importlib
# importlib.reload(dataset)

device = 'cuda' if torch.cuda.is_available() else 'cpu'

class GCN(torch.nn.Module):
    def __init__(self, hidden_channels, data, out_channels = None):
        super().__init__()
        torch.manual_seed(1234567)
        if out_channels is None:
            out_channels = data.y.unique().size(0)
        self.conv1 = torch_geometric.nn.GCNConv(data.num_features, hidden_channels)
        self.conv2 = torch_geometric.nn.GCNConv(hidden_channels, out_channels)

    def forward(self, data):
        x, edge_index, edge_weight = data.x, data.edge_index, data.edge_weight
        x = self.conv1(x, edge_index, edge_weight)
        x = x.relu()
        x = F.dropout(x, p=0.5, training=self.training)
        x = self.conv2(x, edge_index, edge_weight)
        return x
    
def test(model,data):
      model.eval()
      out = model(data)
      pred = out.argmax(dim=1)  # Use the class with highest probability.
      test_correct = pred[~data.test_mask] == data.y[~data.test_mask]  # Check against ground-truth labels.
      test_acc = int(test_correct.sum()) / int((~data.test_mask).sum())  # Derive ratio of correct predictions.
      return test_acc
    
""" get positional coordinates based on the hidden output layer"""
def get_position(model, data):
    model.eval()
    out = model(data)
    node_idx, node_ids = data.node_mapping
    idx_node_mapping = dict(zip(node_idx,node_ids))
    """use T-distributed Stochastic Neighbor Embedding to visualize high-dimensional data"""
    # z = TSNE(n_components=2).fit_transform(out.detach().cpu().numpy())
    z = MulticoreTSNE(n_components=2,n_jobs=1).fit_transform(out.detach().cpu().numpy())
    node_positions = dict(zip(  list(map(lambda x: idx_node_mapping[x], range(data.num_nodes))),
                                list(map(lambda i: {"x":np.float64(z[i,0]),"y":np.float64(z[i,1])} , range(data.num_nodes)))))
    return node_positions  

""" Get GCN Models with pretrained weights"""
def author_area_model(data):
    model = GCN(hidden_channels=16,data=data,out_channels=4)
    model.load_state_dict(torch.load("GNN/GCN_wd_area_weights.pth"))
    return model

def author_subarea_model(data):
    model = GCN(hidden_channels=16,data=data,out_channels=23)
    model.load_state_dict(torch.load("GNN/GCN_wd_subarea_weights.pth"))
    return model

def inst_area_model(data):
    model = GCN(hidden_channels=16,data=data,out_channels=4)
    model.load_state_dict(torch.load("GNN/GCN_wd_inst_area_weights.pth"))
    return model

def inst_subarea_model(data):
    model = GCN(hidden_channels=16,data=data,out_channels=23)
    model.load_state_dict(torch.load("GNN/GCN_wd_inst_weights.pth"))
    return model

