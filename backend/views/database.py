from flask import Blueprint, jsonify, request
from flask_apispec import use_kwargs, doc
from marshmallow import fields
import json
from exceptions import make_swagger_response
import kuzu_db.query_kuzu as query

blueprint_name = "db"

blueprint = Blueprint(blueprint_name, __name__)

def route_path(path):
    return "/api/{}/{}".format(blueprint_name,path)


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
@doc(summary="get computer science area and sub-areas",
     tags=['db'],
     responses=make_swagger_response([]))
@use_kwargs({'conf': fields.Str()})
def get_conference(conf=""):
    if request.method == 'GET':
        conf = request.args.get('conf')

    result = query.get_conference(conf)
    result_json =  json.loads(result.to_json(orient="records"))
    return jsonify(result_json)


""" API example calls """
# http://127.0.0.1:8030/api/db/get_region_mapping
# http://127.0.0.1:8030/api/db/get_area_mapping
# http://127.0.0.1:8030/api/db/get_conference?conf=aaai