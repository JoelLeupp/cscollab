import GNN.gen_dataset as dataset 
import GNN.node_classification as nc 
import requests
import json
import pandas as pd
from torch_geometric.nn import summary

from_year = 2005
region_ids = ["wd"]

""" test author area model """
config = { "from_year": from_year,
            "region_ids":region_ids,
            "strict_boundary":True,
            "institution":False}
use_sub_areas = False
data_area = dataset.get_torch_data(config, use_sub_areas)

model_area = nc.author_area_model(data_area)
nc.test(model_area,data_area) #0.9859320046893317
nc.get_position(model_area,data_area)

print(summary(model_area, data_area))


""" test author subarea model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":False}
use_sub_areas = True
data_subarea = dataset.get_torch_data(config, use_sub_areas)

model_subarea = nc.author_subarea_model(data_subarea)
print(summary(model_subarea, data_subarea))
nc.test(model_subarea,data_subarea) #0.9460726846424384
nc.get_position(model_subarea,data_subarea)

""" test inst area model """
config = { "from_year": 2005,
            "region_ids":["wd"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = False
data_inst_area = dataset.get_torch_data(config, use_sub_areas)

model_inst_area = nc.inst_area_model(data_inst_area)
print(summary(model_inst_area, data_inst_area))
nc.test(model_inst_area,data_inst_area) #0.9629629629629629
nc.get_position(model_inst_area,data_inst_area)

""" test inst subarea model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = True
data_inst_subarea = dataset.get_torch_data(config, use_sub_areas)

model_inst_subarea = nc.inst_subarea_model(data_inst_subarea)
print(summary(model_inst_subarea, data_inst_subarea))
nc.test(model_inst_subarea,data_inst_subarea) #0.8395061728395061
nc.get_position(model_inst_subarea,data_inst_subarea)

# model testing equal top area, subarea == true
local_url = "http://127.0.0.1:8030"
server_url = "https://cscollab.ifi.uzh.ch/backend"
url_base = local_url 

url =  url_base+"/api/db/get_area_mapping"
x = requests.get(url)
area_mapping = pd.DataFrame(json.loads(x.content))

area_ids = list(area_mapping["area-id"].unique())
sub_area_ids = list(area_mapping["sub-area-id"].unique())

def test_multiple(model,data,freq, use_sub_areas):
      model.eval()
      out = model(data)
      pred = out.argmax(dim=1)  # Use the class with highest probability.
      a = "subarea" if use_sub_areas else "area"
      ids = sub_area_ids if use_sub_areas else area_ids
      test_correct = list(map(lambda p, n: ids[p.item()] in freq[n][a]["tops"] , pred, data.node_mapping[1]))
      test_acc = int(sum(test_correct)) / len(pred)  # Derive ratio of correct predictions.
      return test_acc

def validate(model,data):
      model.eval()
      out = model(data)
      pred = out.argmax(dim=1)  # Use the class with highest probability.
      val_correct = pred[data.val_mask] == data.y[data.val_mask]  # Check against ground-truth labels.
      val_acc = int(val_correct.sum()) / int((data.val_mask).sum())  # Derive ratio of correct predictions.
      return val_acc

from_year = 2005
region_ids = ["dach"]

""" test author area model """
url = url_base+"/api/db/get_frequency_research_field"
config={ "from_year": from_year,
            "region_ids":region_ids,
            "strict_boundary":True,
            "institution":False}
input = {"config": json.dumps(config)}
x = requests.post(url, json = input)
freq = json.loads(x.content)

use_sub_areas = False
data_area = dataset.get_torch_data(config, use_sub_areas)

model_area = nc.author_area_model(data_area)
test_multiple(model_area,data_area,freq,use_sub_areas) #0.9885262796289934
validate(model_area,data_area) #0.9878607420980302

""" test author subarea sub area """
use_sub_areas = True
data_subarea = dataset.get_torch_data(config, use_sub_areas)

model_subarea = nc.author_subarea_model(data_subarea)
test_multiple(model_subarea,data_subarea,freq,use_sub_areas) #0.9624871178289248
validate(model_subarea,data_subarea) #0.9507558405863491

""" test inst subarea model """
url = url_base+"/api/db/get_frequency_research_field"
config={ "from_year": from_year,
            "region_ids":region_ids,
            "strict_boundary":True,
            "institution":True}
input = {"config": json.dumps(config)}
x = requests.post(url, json = input)
freq = json.loads(x.content)

use_sub_areas = False
data_area = dataset.get_torch_data(config, use_sub_areas)

model_area = nc.inst_area_model(data_area)
test_multiple(model_area,data_area,freq,use_sub_areas) #0.9179229480737019
validate(model_area,data_area) #0.9553072625698324

""" test inst subarea sub area """
use_sub_areas = True
data_subarea = dataset.get_torch_data(config, use_sub_areas)

model_subarea = nc.inst_subarea_model(data_subarea)
test_multiple(model_subarea,data_subarea,freq,use_sub_areas) #0.8140703517587939
validate(model_subarea,data_subarea) #0.888268156424581