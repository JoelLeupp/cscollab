from flask import Blueprint, jsonify, request
from flask_apispec import use_kwargs, doc, marshal_with
from marshmallow import fields, Schema
import json
from exceptions import make_swagger_response
import kuzudb.query_kuzu as query
from cache import cache

""" define blueprint for database queries"""
blueprint_name = "db"
blueprint = Blueprint(blueprint_name, __name__)

def route_path(path):
    return "/api/{}/{}".format(blueprint_name,path)

# class CsauthorsSchema(Schema):
#     class Meta:
#         fields = ('country_id', 'region_id')

# class ConferenceSchema(Schema):
#     class Meta:
#         fields = ['conf']

@blueprint.route(route_path('get_region_mapping'), methods=['GET'])
@doc(summary="get country region mapping",
     tags=['db'],
    responses=make_swagger_response([]))
def get_region_mapping():
    result = query.get_region_mapping()
    result_json =  json.loads(result.to_json(orient="records"))
    return jsonify(result_json)


@blueprint.route(route_path('get_area_mapping'), methods=['GET'])
@doc(summary="get computer science area and sub-areas",
     tags=['db'],
     responses=make_swagger_response([]))
def get_area_mapping():
    result = query.get_area_mapping()
    result_json =  json.loads(result.to_json(orient="records"))
    return jsonify(result_json)


@blueprint.route(route_path('get_conference'), methods=['GET', 'POST'])
@doc(summary="get all in/proceedings from a conference",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'conf': fields.Str()})
# @marshal_with(ConferenceSchema())
def get_conference(**kwargs):
    if request.method == 'GET':
        conf = request.args.get('conf')
    else:
        conf = kwargs['conf']
        
    cache_key = "get_conference_{}".format(conf)
    result_json = cache.get(cache_key)
    if (conf is not None) and (result_json is None):
        result = query.get_conference(conf=conf)
        result_json =  json.loads(result.to_json(orient="records"))
        cache.set(cache_key, result_json)
    return jsonify(result_json)


@blueprint.route(route_path('get_csauthors'), methods=['GET', 'POST'])
@doc(summary="get authors from csranking with their affiliation filtered on region/country",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'country_id': fields.Str(default=None),
             'region_id': fields.Str(default="wd")})
# @marshal_with(CsauthorsSchema())
def get_csauthors(**kwargs):
    if request.method == 'GET':
        country_id = request.args.get('country_id')
        region_id = request.args.get('region_id',"wd")

    result = query.get_csauthors(**kwargs)
    result_json =  json.loads(result.to_json(orient="records"))
    return jsonify(result_json)


# @blueprint.route(route_path('get_collaboration'), methods=['POST'])
# @doc(summary="get collaboration of author/institution",
#     description =   """get collaboration of author/institution filtered on region and area \n
#             example: config =    {  "area_id" : "ai", \n
#                                     "area_type":  "a", \n
#                                     "region_id":"dach",\n
#                                     "country_id":None,\n
#                                     "strict_boundary":True}""",
#      tags=['db'],
#      responses=make_swagger_response([]))
# @use_kwargs({'config': fields.Str(default="{}")})
# def get_collaboration(**kwargs):
#     config = json.loads(kwargs['config'])
#     result = query.get_collaboration(collab_config=config)
#     result_json =  json.loads(result.to_json(orient="records"))
#     return jsonify(result_json)

@blueprint.route(route_path('get_collaboration'), methods=['POST'])
@doc(summary="get filtered collaboration of author/institution",
    description =   """get collaboration of author/institution filtered on region and area and year\n
            example: config = { "from_year": 2005,\n
                                "to_year": 2023,    \n
                                "area_ids" : ["ai","systems"], \n
                                "sub_area_ids":  ["robotics","bio"], \n
                                "region_ids":["europe","northamerica"],\n
                                "country_ids":["jp","sg"],\n
                                "strict_boundary":True
                                }""",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'config': fields.Str(default="{}")})
def get_collaboration(**kwargs):
    config = json.loads(kwargs['config'])
    collab = query.get_flat_collaboration(ignore_area=False)
    cache_key = "get_collaboration_{}".format(config)
    result_json = cache.get(cache_key)
    if result_json is None:
        result = query.filter_collab(collab,config)
        result_json =  json.loads(result.to_json(orient="records"))
        cache.set(cache_key, result_json)
    return jsonify(result_json)

@blueprint.route(route_path('get_flat_collaboration'), methods=['POST'])
@doc(summary="get collaboration of authors inclusing country institution and area information in a flat structure",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'ignore_area': fields.Boolean(default=False)})
def get_flat_collaboration(**kwargs):
    ignore_area = kwargs.get('ignore_area',False)
    result = query.get_flat_collaboration(ignore_area=ignore_area)
    result_json =  json.loads(result.to_json(orient="records"))
    # cache_key = "get_flat_collaboration_{}".format(str(ignore_area))
    # result_json = cache.get(cache_key)
    # if result_json is None:
    #     result = query.get_flat_collaboration(ignore_area=ignore_area)
    #     result_json =  json.loads(result.to_json(orient="records"))
    #     cache.set(cache_key, result_json)
    return jsonify(result_json)


@blueprint.route(route_path('get_weighted_collab'), methods=['POST'])
@doc(summary="get weighted collaboration of author/institution",
    description =   """wrapper for the combination of get_collaboration() and weighted_collab()\n
        example :config =     { "from_year": 2005,\n
                                "to_year": 2023,\n
                                "area_ids" : ["ai","systems"], \n
                                "sub_area_ids":  ["robotics","bio"], \n
                                "region_ids":["europe","northamerica"],\n
                                "country_ids":["jp","sg"],\n
                                "strict_boundary":True,\n
                                "institution":False}""",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'config': fields.Str(default="{}")})
def get_weighted_collab(**kwargs):
    config = json.loads(kwargs.get('config',"{}"))
    cache_key = "get_weighted_collab_{}".format(config)
    result_json = cache.get(cache_key)
    if result_json is None:
        # result = query.get_weighted_collab(config=config)
        collab = query.get_flat_collaboration(ignore_area=False)
        collab_filtered = query.filter_collab(collab,config)
        institution = config.get("institution")
        result = query.weighted_collab(collab_filtered,institution=institution)
        result_json =  json.loads(result.to_json(orient="records"))
        cache.set(cache_key, result_json)
    return jsonify(result_json)

@blueprint.route(route_path('get_frequency_research_field'), methods=['POST'])
@doc(summary="get the frequeny and top area and sub area for each author/institution of a given collaboration network",
    description =   """frequeny counter\n
        example :config =     { "from_year": 2005,\n
                                "to_year": 2023,\n
                                "area_ids" : ["ai","systems"], \n
                                "sub_area_ids":  ["robotics","bio"], \n
                                "region_ids":["europe","northamerica"],\n
                                "country_ids":["jp","sg"],\n
                                "strict_boundary":True,\n
                                "institution":False}""",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'config': fields.Str(default="{}")})
def get_frequency_research_field(**kwargs):
    config = json.loads(kwargs.get('config',"{}"))
    cache_key = "get_frequency_research_field_{}".format(config)
    result_json = cache.get(cache_key)
    if result_json is None:
        collab = query.get_flat_collaboration(ignore_area=False)
        collab_filtered = query.filter_collab(collab,config)
        institution = config.get("institution")
        result_json = query.get_top_research_field(collab_filtered,institution=institution)
        cache.set(cache_key, result_json)
    return jsonify(result_json)

@blueprint.route(route_path('get_publications_node'), methods=['POST'])
@doc(summary="get all publications of a node based on the config",
    description =   """
        example :config =     { "from_year": 2005,\n
                                "to_year": 2023,\n
                                "area_ids" : ["ai","systems"], \n
                                "sub_area_ids":  ["robotics","bio"], \n
                                "region_ids":["europe","northamerica"],\n
                                "country_ids":["jp","sg"],\n
                                "strict_boundary":True,\n
                                "institution":False}""",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'node':fields.Str(), 'config': fields.Str(default="{}")})
def get_publications_node(**kwargs):
    config = json.loads(kwargs.get('config',"{}"))
    node = kwargs.get('node')
    cache_key = "get_publications_node_{}_{}".format(config,node)
    result_json = cache.get(cache_key)
    if result_json is None:
        collab = query.get_flat_collaboration(ignore_area=False)
        collab_filtered = query.filter_collab(collab,config)
        institution = config.get("institution")
        result = query.get_publications_node(node, collab_filtered, institution = institution)
        result_json =  json.loads(result.to_json(orient="records"))
        cache.set(cache_key, result_json)
    return jsonify(result_json)


@blueprint.route(route_path('get_publications_edge'), methods=['POST'])
@doc(summary="get all publications of an edge based on the config",
    description =   """
        example :config =     { "from_year": 2005,\n
                                "to_year": 2023,\n
                                "area_ids" : ["ai","systems"], \n
                                "sub_area_ids":  ["robotics","bio"], \n
                                "region_ids":["europe","northamerica"],\n
                                "country_ids":["jp","sg"],\n
                                "strict_boundary":True,\n
                                "institution":False}""",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'edge':fields.List(fields.Str()), 'config': fields.Str(default="{}")})
def get_publications_edge(**kwargs):
    config = json.loads(kwargs.get('config',"{}"))
    edge = kwargs.get('edge')
    cache_key = "get_publications_node_{}_{}".format(config,edge)
    result_json = cache.get(cache_key)
    if result_json is None:
        collab = query.get_flat_collaboration(ignore_area=False)
        collab_filtered = query.filter_collab(collab,config)
        institution = config.get("institution")
        result = query.get_publications_edge(edge, collab_filtered, institution = institution)
        result_json =  json.loads(result.to_json(orient="records"))
        cache.set(cache_key, result_json)
    return jsonify(result_json)



# @blueprint.route(route_path('get_collab_pid'), methods=['POST'])
# @doc(summary="get all the collaborations between two authors with the constraints given in the config",
#     description =   """example :config =    {  "from_year": 2010,\n
#                                                 "area_id" : "ai", \n
#                                                 "area_type":  "a"}""",
#      tags=['db'],
#      responses=make_swagger_response([]))
# @use_kwargs({'config': fields.Str(default="{}"),
#              'pid_x':fields.Str(),
#              'pid_y':fields.Str()})
# def get_collab_pid(**kwargs):
#     config = json.loads(kwargs['config'])
#     pid_x = kwargs['pid_x']
#     pid_y = kwargs['pid_y']
#     cache_key = "get_collab_pid_{}_{}_{}".format(pid_x,pid_y,kwargs['config'])
#     result_json = cache.get(cache_key)
#     if (pid_x and pid_y and config) and (result_json is None):
#         result = query.get_collab_pid(pid_x, pid_y, config=config)
#         result_json =  json.loads(result.to_json(orient="records"))
#         cache.set(cache_key, result_json)
#     return jsonify(result_json)


# @blueprint.route(route_path('get_collab_institution'), methods=['POST'])
# @doc(summary="get all the collaborations between two authors with the constraints given in the config",
#     description =   """example :config =    {  "from_year": 2010,\n
#                                                 "area_id" : "ai", \n
#                                                 "area_type":  "a"}""",
#      tags=['db'],
#      responses=make_swagger_response([]))
# @use_kwargs({'config': fields.Str(default="{}"),
#              'inst_x':fields.Str(),
#              'inst_y':fields.Str()})
# def get_collab_institution(**kwargs):
#     config = json.loads(kwargs['config'])
#     inst_x = kwargs['inst_x']
#     inst_y = kwargs['inst_y']
#     cache_key = "get_collab_institution_{}_{}_{}".format(inst_x,inst_y,kwargs["config"])
#     result_json = cache.get(cache_key)
#     if (inst_x and inst_y and config) and (result_json is None):
#         result = query.get_collab_institution(inst_x, inst_y, config=config)
#         result_json =  json.loads(result.to_json(orient="records"))
#         cache.set(cache_key, result_json)
#     return jsonify(result_json)