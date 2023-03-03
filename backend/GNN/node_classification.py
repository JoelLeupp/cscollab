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

""" test author area model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":False}
use_sub_areas = False
data_area = dataset.get_torch_data(config, use_sub_areas)

model_area = author_area_model(data_area)
test(model_area,data_area) #0.9859320046893317

""" test author subarea model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":False}
use_sub_areas = True
data_subarea = dataset.get_torch_data(config, use_sub_areas)

model_subarea = author_subarea_model(data_subarea)
test(model_subarea,data_subarea) #0.9460726846424384

""" test inst area model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = False
data_inst_area = dataset.get_torch_data(config, use_sub_areas)

model_inst_area = inst_area_model(data_inst_area)
test(model_inst_area,data_inst_area) #0.9629629629629629

""" test inst subarea model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = True
data_inst_subarea = dataset.get_torch_data(config, use_sub_areas)

model_inst_subarea = inst_subarea_model(data_inst_subarea)
test(model_inst_subarea,data_inst_subarea) #0.8395061728395061