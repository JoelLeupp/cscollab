
"""Create an application instance."""

from flask.helpers import get_debug_flag
from settings import ProdConfig, DevConfig
from app import create_app

config = DevConfig

"""Create Backend"""
app = create_app(config)

"""Start backend"""
if __name__ == "__main__":
    app.run("localhost", port=8030, debug=config.DEBUG)
