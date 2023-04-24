from flask import Blueprint, jsonify, request
from flask_apispec import use_kwargs, doc, marshal_with
from marshmallow import fields, Schema
import json
from exceptions import make_swagger_response
import kuzudb.query_kuzu as query
import analytics.statistics as stat
from cache import cache
import pandas as pd
import GNN.gen_dataset as dataset 
import GNN.node_classification as nc 

""" define blueprint for graph analytics"""
blueprint_name = "gcn"
blueprint = Blueprint(blueprint_name, __name__)

def route_path(path):
    return "/api/{}/{}".format(blueprint_name,path)

@blueprint.route(route_path('get_node_position'), methods=['POST'])
@doc(summary="get the position of the nodes based on the hidden activations of the GCN model",
    description =   """get position of nodes""",
     tags=['gcn'],
     responses=make_swagger_response([]))
@use_kwargs({'config': fields.Str(default="{}"),'sub_areas': fields.Boolean(default=True)})
def get_node_position(**kwargs):
    config = json.loads(kwargs.get('config',"{}"))
    use_sub_areas = kwargs.get('sub_areas',True)
    institution = config.get("institution")
    cache_key = "get_node_position_{}_{}".format(config,use_sub_areas)
    positions = cache.get(cache_key)
    if positions is None:
        data = dataset.get_torch_data(config, use_sub_areas,use_cache=True)
        """choose the right pretrained model based on the inputs"""
        if institution:
            if use_sub_areas:
                model = nc.inst_subarea_model(data)
            else:
                model = nc.inst_area_model(data)
        else:
            if use_sub_areas:
                model = nc.author_subarea_model(data)
            else:
                model = nc.author_area_model(data)
        """get the positions of the nodes"""
        positions = nc.get_position(model,data)
        cache.set(cache_key, positions)
    return jsonify(positions)

