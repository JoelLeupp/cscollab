# Backend Server 

## Virtual Environment 

Use *backend/* as the working directory and always activate the virutal environment of this directory

```{shell}
virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
```

# Start the Server

The server will run at http://127.0.0.1:8030 and the swagger API documentation can be found at //swagger-ui

```{shell}
source venv/bin/activate
make server
```
