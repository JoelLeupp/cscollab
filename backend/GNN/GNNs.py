import numpy as np
import pathpy as pp
import torch
import torch_geometric
from matplotlib import pyplot as plt
import matplotlib as mpl
import seaborn as sns
import pandas as pd
from sklearn.decomposition import TruncatedSVD
from GNN.gen_dataset import collab_to_torch

plt.style.use('default')
sns.set_style("whitegrid")

device = 'cuda' if torch.cuda.is_available() else 'cpu'
print('Running on', device)

# source, target = data.edge_index
# torch_geometric.utils.degree(source, data.num_nodes, dtype=torch.float32)
# torch_geometric.utils.normalized_cut(data.edge_index, data.edge_weight, data.num_nodes)

class GraphConvolution(torch_geometric.nn.MessagePassing):

    def __init__(self, in_ch, out_ch):
        super().__init__(aggr='add')

        # this linear function is used to transform node features 
        # into messages that are then "sent" to neighbors
        self.linear = torch.nn.Linear(in_ch, out_ch)
        
    def forward(self, x, edge_index):
        """
        This function uses the edges captured in edge_index, performs 
        the graph convolution function according to (Kipf, Welling 2017)
        and propagates the transformed features along the edges of the graph
        """

        # we linearly transform the features of *all* nodes stored in x
        x = self.linear(x)

        # extract source and target nodes of all edges
        source, target = edge_index
        
        # compute the (in-)degrees $d_i$ of source nodes
        deg = torch_geometric.utils.degree(target, x.size(0), dtype=x.dtype)
        deg_inv_sqrt = deg.pow(-0.5)
        deg_inv_sqrt[deg_inv_sqrt == float('inf')] = 0

        # with this, the normalization to be applied in the propagation step can be expressed as
        # this corresponds to D^{-0.5} A * D^{-0.5} in (Kipf, Welling 2017)
        norm = deg_inv_sqrt[source] * deg_inv_sqrt[target]
        
        # the propagate function propagates messages along the edges of the graph
        # this function internally calls the functions: message(), aggregate() and update()
        # the normalization is applied in the message() function
        return self.propagate(edge_index, x=x, norm=norm)
    
    def message(self, x_j, norm):
        # x_j is a so-called **lifted** tensor which contains the source node features of each edge, 
        # i.e. it has a shape (m, out_ch) where m is the number of edges

        # a call to view(-1, 1) returns a reshaped tensor, where the second dimension 
        # is one and the first dimension is inferred automatically
        return norm.view(-1,1) * x_j

class GCN(torch.nn.Module):

    def __init__(self, data: torch_geometric.data.Data, out_ch, hidden_dim=16):
        super().__init__()

        # first convolution layer 
        self.input_to_hidden = GraphConvolution(data.num_node_features, hidden_dim)

        # second convolution layer
        self.hidden_to_output =  GraphConvolution(hidden_dim, out_ch)
        
    def forward(self, x, edge_index):
        
        # first graph convolution -> map nodes to representations in hidden_dim dimensions
        x = self.input_to_hidden(x, edge_index)

        # non-linear activation function
        x = torch.sigmoid(x)

        # second graph convolution -> maps node representations to output classes
        x = self.hidden_to_output(x, edge_index)

        # output class probabilities
        return torch.sigmoid(x)


config = { "from_year": 2015,
            "region_id":"dach",
            "strict_boundary":True,
            "institution":True}

data = collab_to_torch(config,weighted=False)
transform = torch_geometric.transforms.RandomNodeSplit(split='train_rest', num_val=0.3, num_test=0)
transform(data)
print(f'Has self-loops: {data.has_self_loops()}')
print(f'Is undirected: {data.is_undirected()}')

model = GCN(data, out_ch=1, hidden_dim=4)

epochs = 200
lrn_rate = 0.1

optimizer = torch.optim.SGD(model.parameters(), lr=lrn_rate)

criterion = torch.nn.CrossEntropyLoss()

indices = np.arange(data.num_nodes)
losses = []
model.train()
for epoch in range(epochs):

    error = 0
    
    np.random.shuffle(indices)
    for i in indices:

        if data.train_mask[i]:

            # set gradients to zero
            optimizer.zero_grad()

            # compute loss function for training sample and backpropagate
            output = model(data.x, data.edge_index)
            loss = torch.nn.functional.binary_cross_entropy(output[i], data.y[i])
            # loss = criterion(output[data.train_mask], data.y[data.train_mask])
            loss.backward()

            # update parameters
            optimizer.step()

            error += loss.detach().numpy()

    losses.append(error)

# plot evolution of loss function
plt.plot(range(epochs), losses)

output = model.forward(data.x, data.edge_index)

# we efficiently map probabilities to classes by rounding values to the 
# nearest integer, i.e. we obtain class 0 for probabilities smaller than 0.5
# and class 1 for probabilities larger than 0.5
prediction = output

true_prediction = [data.y[x].argmax().item()==prediction[x].argmax().item() for x in range(data.num_nodes)]
accuracy=sum(true_prediction)/len(true_prediction)

