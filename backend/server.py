#----------------------------------------
# Create an application instance
#----------------------------------------

from flask.helpers import get_debug_flag
from settings import ProdConfig, DevConfig
from app import create_app

# set configuration Production or Development
config = DevConfig

app = create_app(config)

# start server
if __name__ == "__main__":
    app.run("127.0.0.1", port=8030, debug=config.DEBUG)
