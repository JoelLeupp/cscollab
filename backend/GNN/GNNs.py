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
from torch.nn import Linear
import torch.nn.functional as F
from GNN.gen_dataset import collab_to_torch

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
    


""" test GNN model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":False}
collab_flat = query.get_flat_collaboration(ignore_area=False,use_cache=False)
collab_filtered = query.filter_collab(collab_flat,config)


