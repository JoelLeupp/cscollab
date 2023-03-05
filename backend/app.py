from flask import Flask
from settings import ProdConfig, DevConfig
from exceptions import InvalidUsage
from swagger_spec import register_api
from flask_cors import CORS
from cache import cache
from views import common
from views import swagger
from views import database
from views import analytics
from views import gcn


# Register Flask blueprints
def register_blueprints(app):
    
    # access control for origins
    cors = CORS()
    origins = app.config.get("CORS_ORIGIN_WHITELIST","*")
    cors.init_app(common.blueprint, origins=origins)
    cors.init_app(database.blueprint, origins=origins)
    cors.init_app(analytics.blueprint, origins=origins)
    cors.init_app(gcn.blueprint, origins=origins)
    
    app.register_blueprint(common.blueprint)
    app.register_blueprint(database.blueprint)
    app.register_blueprint(analytics.blueprint)
    app.register_blueprint(gcn.blueprint)
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
