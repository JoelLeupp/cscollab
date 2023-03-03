import GNN.gen_dataset as dataset 
import GNN.node_classification as nc 


""" test author area model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":False}
use_sub_areas = False
data_area = dataset.get_torch_data(config, use_sub_areas)

model_area = nc.author_area_model(data_area)
nc.test(model_area,data_area) #0.9859320046893317
nc.get_position(model_area,data_area)

""" test author subarea model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":False}
use_sub_areas = True
data_subarea = dataset.get_torch_data(config, use_sub_areas)

model_subarea = nc.author_subarea_model(data_subarea)
nc.test(model_subarea,data_subarea) #0.9460726846424384
nc.get_position(model_subarea,data_subarea)

""" test inst area model """
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = False
data_inst_area = dataset.get_torch_data(config, use_sub_areas)

model_inst_area = nc.inst_area_model(data_inst_area)
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
nc.test(model_inst_subarea,data_inst_subarea) #0.8395061728395061
nc.get_position(model_inst_subarea,data_inst_subarea)

