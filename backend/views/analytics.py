from flask import Blueprint, jsonify, request
from flask_apispec import use_kwargs, doc, marshal_with
from marshmallow import fields, Schema
import json
from exceptions import make_swagger_response
import kuzudb.query_kuzu as query
import analytics.statistics as stat
from cache import cache

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
    nodes = kwargs["nodes"]
    edges = kwargs["edges"]
    weighted = kwargs.get("weighted",False)
    top = kwargs.get("top",5)
    
    G = stat.gen_graph(nodes, edges, weighted=weighted)
    statistics =  stat.get_statistics(G)
    centralities = stat.get_centralities(G, weighted=weighted, top=top)
    analytics={"statistics":statistics,
               "centralities":centralities}
   
    return jsonify(analytics)

