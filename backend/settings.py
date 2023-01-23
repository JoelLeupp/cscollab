import os
from apispec import APISpec
from apispec.ext.marshmallow import MarshmallowPlugin

class Config(object):
    """Base configuration."""
    APP_DIR = os.path.abspath(os.path.dirname(__file__))  # This directory
    #STATIC_FOLDER = "./public"
    APISPEC_SWAGGER_UI_URL = None
    APISPEC_SPEC = APISpec(
        title='BACKEND APIS',
        openapi_version='3.0.2',
        info=dict(description="A minimal example of a swagger API documentation"),
        version='v1',
        plugins=[MarshmallowPlugin()],
    )
    CACHE_TYPE = 'SimpleCache',
    CACHE_DEFAULT_TIMEOUT= 300

class ProdConfig(Config):
    """Production configuration."""
    DEBUG = False
    ENV = "prod"

class DevConfig(Config):
    """Development configuration."""
    DEBUG = True
    ENV = "dev"


