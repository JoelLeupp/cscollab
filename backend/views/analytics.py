from flask import Blueprint, jsonify, request
from flask_apispec import use_kwargs, doc, marshal_with
from marshmallow import fields, Schema
import json
from exceptions import make_swagger_response
import kuzudb.query_kuzu as query
import analytics.statistics as stat
from cache import cache
import pandas as pd

""" define blueprint for graph analytics"""
blueprint_name = "analytics"
blueprint = Blueprint(blueprint_name, __name__)

def route_path(path):
    return "/api/{}/{}".format(blueprint_name,path)


@blueprint.route(route_path('get_analytics'), methods=['POST'])
@doc(summary="get core graph statistics and centrality scores",
     tags=['analytics'],
     responses=make_swagger_response([]))
@use_kwargs({'nodes': fields.List(fields.Raw),
            'edges': fields.List(fields.List(fields.Raw)),
            'weighted': fields.Boolean(default=False),
            'top': fields.Int(default=5)})
def get_analytics(**kwargs):
    nodes = kwargs['nodes']
    edges = kwargs['edges']
    weighted = kwargs.get('weighted',False)
    top = kwargs.get('top',5)
    
    """crate networkx graph and get analytics"""
    G = stat.gen_graph(nodes, edges, weighted=weighted)
    statistics =  stat.get_statistics(G)
    centralities = stat.get_centralities(G, weighted=weighted, top=top)
    analytics={"statistics":statistics,
               "centralities":centralities}
    return jsonify(analytics)

@blueprint.route(route_path('get_analytics_collab'), methods=['POST'])
@doc(summary="get core graph statistics and centrality scores",
     tags=['analytics'],
     responses=make_swagger_response([]))
@use_kwargs({   'config': fields.Str(default="{}"),
                'top': fields.Int(default=5)})
def get_analytics_collab(**kwargs):
    config = json.loads(kwargs.get('config',"{}"))
    top = kwargs.get('top',5)
  
    cache_key = "get_analytics_collab_{}_{}".format(config,top)
    analytics = cache.get(cache_key)
    if analytics is not None:
        return jsonify(analytics)
    
    """ get weighted collaboration based on config"""
    cache_key_collab = "get_weighted_collab_{}".format(config)
    weighted_collab = cache.get(cache_key_collab)
    if weighted_collab is None:
        collab = query.get_flat_collaboration(ignore_area=False)
        collab_filtered = query.filter_collab(collab,config)
        institution = config.get("institution")
        result = query.weighted_collab(collab_filtered,institution=institution)
        weighted_collab =  json.loads(result.to_json(orient="records"))
        cache.set(cache_key_collab, weighted_collab)
    
    """craete nodes and edges"""
    data = pd.DataFrame(weighted_collab)
    nodes = list(set(data["a"]) | set(data["b"]))
    edges=[tuple(row.values) for _,row in data.iterrows()]
    
    """crate networkx graph and get analytics"""
    G = stat.gen_graph(nodes, edges, weighted=True)
    statistics =  stat.get_statistics(G)
    centralities = stat.get_centralities(G, weighted=True, top=top)
    analytics={"statistics":statistics,
               "centralities":centralities}
    cache.set(cache_key, analytics)
    return jsonify(analytics)

