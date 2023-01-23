from flask_caching import Cache

"""init cache"""
cache = Cache(config={'CACHE_TYPE': 'SimpleCache',
                      'CACHE_DEFAULT_TIMEOUT': 300})

"""clear cache"""
# cache.clear()
