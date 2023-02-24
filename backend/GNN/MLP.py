import numpy as np
import pathpy as pp
import torch
from torch.nn import Linear
import torch.nn.functional as F
import torch_geometric
from matplotlib import pyplot as plt
import matplotlib as mpl
import seaborn as sns
import pandas as pd
from sklearn.manifold import TSNE
from sklearn.decomposition import TruncatedSVD
from GNN.gen_dataset import collab_to_torch

plt.style.use('default')
sns.set_style("whitegrid")

device = 'cuda' if torch.cuda.is_available() else 'cpu'
print('Running on', device)

config = { "from_year": 2015,
            "region_id":"dach",
            "strict_boundary":True,
            "institution":True}

data = collab_to_torch(config,only_features=True,y_method=2)
data.y = data.y.long()


class MLP(torch.nn.Module):
    def __init__(self, hidden_channels):
        super().__init__()
        torch.manual_seed(12345)
        self.lin1 = Linear(data.num_features, hidden_channels)
        self.lin2 = Linear(hidden_channels, 4)

    def forward(self, x):
        x = self.lin1(x)
        x = x.relu()
        x = F.dropout(x, p=0.5, training=self.training)
        x = self.lin2(x)
        return x

model = MLP(hidden_channels=16)
print(model)

criterion = torch.nn.CrossEntropyLoss()  # Define loss criterion.
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)  # Define optimizer.

def train():
      model.train()
      optimizer.zero_grad()  # Clear gradients.
      out = model(data.x)# Perform a single forward pass.
      loss = criterion(out[data.train_mask], data.y[data.train_mask])  # Compute the loss solely based on the training nodes.
      loss.backward()  # Derive gradients.
      optimizer.step()  # Update parameters based on gradients.
      return loss

def test():
      model.eval()
      out = model(data.x)
      pred = out.argmax(dim=1)  # Use the class with highest probability.
      test_correct = pred[~data.test_mask] == data.y[~data.test_mask]  # Check against ground-truth labels.
      test_acc = int(test_correct.sum()) / int((~data.test_mask).sum())  # Derive ratio of correct predictions.
      return test_acc

for epoch in range(0, 1000):
    loss = train()
    print(f'Epoch: {epoch:03d}, Loss: {loss:.4f}')
    
test_acc = test()
print(f'Test Accuracy: {test_acc:.4f}')

class GCN(torch.nn.Module):
    def __init__(self, hidden_channels):
        super().__init__()
        torch.manual_seed(1234567)
        self.conv1 = torch_geometric.nn.GCNConv(data.num_features, hidden_channels)
        self.conv2 = torch_geometric.nn.GCNConv(hidden_channels, 4)

    def forward(self, x, edge_index):
        x = self.conv1(x, edge_index)
        x = x.relu()
        x = F.dropout(x, p=0.5, training=self.training)
        x = self.conv2(x, edge_index)
        return x

model = GCN(hidden_channels=16)
print(model)

model = GCN(hidden_channels=16)
model.eval()


def visualize(h, color):
    z = TSNE(n_components=2).fit_transform(h.detach().cpu().numpy())

    plt.figure(figsize=(10,10))
    plt.xticks([])
    plt.yticks([])

    plt.scatter(z[:, 0], z[:, 1], s=70, c=color, cmap="Set2")
    plt.show()
    
out = model(data.x, data.edge_index)
visualize(out, color=data.y)

model = GCN(hidden_channels=16)
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)
criterion = torch.nn.CrossEntropyLoss()

def train():
      model.train()
      optimizer.zero_grad()  # Clear gradients.
      out = model(data.x, data.edge_index)  # Perform a single forward pass.
      loss = criterion(out[data.train_mask], data.y[data.train_mask])  # Compute the loss solely based on the training nodes.
      loss.backward()  # Derive gradients.
      optimizer.step()  # Update parameters based on gradients.
      return loss

def test():
      model.eval()
      out = model(data.x, data.edge_index)
      pred = out.argmax(dim=1)  # Use the class with highest probability.
      test_correct = pred[~data.test_mask] == data.y[~data.test_mask]  # Check against ground-truth labels.
      test_acc = int(test_correct.sum()) / int((~data.test_mask).sum())  # Derive ratio of correct predictions.
      return test_acc


for epoch in range(0, 50000):
    loss = train()
    print(f'Epoch: {epoch:03d}, Loss: {loss:.4f}')
    
test_acc = test()
print(f'Test Accuracy: {test_acc:.4f}')