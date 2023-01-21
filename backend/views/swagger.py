from swagger_ui_bundle import swagger_ui_path

from flask import Flask, Blueprint, send_from_directory, render_template

blueprint = Blueprint(
    'swagger_ui',
    __name__,
    static_url_path='',
    static_folder=swagger_ui_path,
    template_folder=swagger_ui_path
)

SWAGGER_UI_CONFIG = {
    "openapi_spec_url": "/swagger/" #https://petstore.swagger.io/v2/swagger.json"
}

@blueprint.route('/swagger-ui')
def swagger_ui_index():
    return render_template('index.j2', **SWAGGER_UI_CONFIG)

