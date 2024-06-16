#REST API
#flask
from flask import Flask
from flask_restful import Resource, Api, reqparse, abort

#Flask 인스턴스 정리
app = Flask(__name__)
api = Api(app)

# 웹 화면에 띄워줄 Json => info
converted_foodinfo = info["result"]
converted_foodrecipe = recipe["result"]

#할 일 목록들
Todos = {
    'todo1': {"task": converted_foodinfo},
    'todo2': {'task': converted_foodrecipe}
}

#Json을 반환하는 클래스
class food_info(Resource):
    def get(self):
        return Todos

api.add_resource(food_info, '/todos/')

if __name__ == "__main__":
    #app.run(debug=True, host='0.0.0.0', port=8090)
    app.run(host='0.0.0.0', port=8090, debug=True)