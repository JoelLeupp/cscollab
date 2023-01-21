from flask import jsonify
import operator as op

def template(data, code=500):
    return {'message': {'errors': {'body': data}}, 'status_code': code}

def missing_arguments(xs):
    return template(
        ['Arguments is/are missing: %s'.format(', '.join(xs))], code=422)

UNKNOWN_ERROR = template([], code=500)

class InvalidUsage(Exception):
    status_code = 500

    def __init__(self, message, status_code=None, payload=None):
        Exception.__init__(self)
        self.message = message
        if status_code is not None:
            self.status_code = status_code
        self.payload = payload

    def to_json(self):
        rv = self.message
        return jsonify(rv)

    @classmethod
    def unknown_error(cls):
        return cls(**UNKNOWN_ERROR)

def make_swagger_response(method_names, d=None):
    res = dict(d) if d else {}
    res["200"] = {"description": "Ok."}
    for m in method_names:
        data = getattr(InvalidUsage, m)()
        message = data.message["errors"]["body"][0]
        code = str(data.status_code)
        res[code] = {"description": message}
    return res