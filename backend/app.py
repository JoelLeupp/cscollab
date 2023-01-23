from flask import Flask
from settings import ProdConfig, DevConfig
from exceptions import InvalidUsage
from swagger_spec import register_api
from cache import cache
from views import common
from views import swagger
from views import database

# Register Flask blueprints
def register_blueprints(app):
    app.register_blueprint(common.blueprint)
    app.register_blueprint(database.blueprint)
    app.register_blueprint(swagger.blueprint)

def register_errorhandlers(app):

    def errorhandler(error):
        response = error.to_json()
        response.status_code = error.status_code
        return response

    app.errorhandler(InvalidUsage)(errorhandler)

# An application factory, as explained here:
# http://flask.pocoo.org/docs/patterns/appfactories/.
# :param config_object: The configuration object to use.
def create_app(config_object=DevConfig):

    
    app = Flask(__name__)
    app.static_url_path = ''
    app.url_map.strict_slashes = False
    app.config.from_object(config_object)
    cache.init_app(app)
    register_blueprints(app)
    register_errorhandlers(app)
    app.__docs__ = register_api(app)
    return app
