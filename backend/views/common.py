from flask import Blueprint, jsonify, request
from flask_apispec import use_kwargs, doc
from marshmallow import fields
import json
from exceptions import make_swagger_response

blueprint = Blueprint('common', __name__)


@blueprint.route('/api/', methods=['GET'])
@doc(summary="Test for debug",
     tags=['test'])
def debug():
    return jsonify({"message": "Debug"})


@blueprint.route('/api/ping/<n>', methods=['GET'])
@doc(summary="ping a number",
     tags=['test'],
     responses=make_swagger_response([]))
def ping(n):
    return jsonify({"message": n})


@blueprint.route('/api/echo', methods=['GET', 'POST'])
@doc(summary="Give the echo back",
     description="Some descriptions",
     tags=['test'],
     responses=make_swagger_response([]))
@use_kwargs({'echo': fields.Str(default="HELLO WORLD")})
def get_echo(echo=""):
    if request.method == 'GET':
        e = request.args.get('echo','HELLO WORLD')
    else:
        e = echo
    return jsonify({"message": e})
