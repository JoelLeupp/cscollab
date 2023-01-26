# syntax=docker/dockerfile:1

# get a docker python container
FROM python:3.8-slim-buster

WORKDIR /python-docker

# install requirements
COPY requirements.txt requirements.txt
RUN pip3 install -r requirements.txt

# copy files into container
COPY . .

# run server
ENTRYPOINT ["./gunicorn.sh"]
