from flask_apispec import FlaskApiSpec
import views.common as common
import views.database as database
import views.analytics as analytics
import views.gcn as gcn

VIEWS = [database,gcn,analytics]


def get_registered_function(module):
    return [f for f in dir(module) if '__apispec__' in dir(getattr(module, f))]


def register_api_module(docs, module, targets):
    for target in targets:
        docs.register(getattr(module, target), blueprint=module.blueprint.name)
    return docs


def register_documentation(docs, modules):
    for module in modules:
        targets = get_registered_function(module)
        register_api_module(docs, module, targets)


def register_api(app):
    docs = FlaskApiSpec(app)
    register_documentation(docs, VIEWS)

    # remove the options method in the swagger doc
    for key, value in docs.spec._paths.items():
        docs.spec._paths[key] = {
            inner_key: inner_value
            for inner_key, inner_value in value.items()
            if inner_key != 'options'
        }
    return docs

