import os
from apispec import APISpec
from apispec.ext.marshmallow import MarshmallowPlugin

class Config(object):
    """Base configuration."""
    APP_DIR = os.path.abspath(os.path.dirname(__file__))  # This directory
    #STATIC_FOLDER = "./public"
    APISPEC_SWAGGER_UI_URL = None
    APISPEC_SPEC = APISpec(
        title='CSCOLLAB BACKEND',
        openapi_version='3.0.2',
        info=dict(description="""Swagger documentation of APIs provided by the cscollab backend used for the Master Thesis
                  'Interactive Visualization of Scientific Collaboration Networks based on Graph Neural Networks'"""),
        version='v1',
        plugins=[MarshmallowPlugin()],
    )

class ProdConfig(Config):
    """Production configuration."""
    DEBUG = False
    ENV = "prod"

class DevConfig(Config):
    """Development configuration."""
    DEBUG = True
    ENV = "dev"


