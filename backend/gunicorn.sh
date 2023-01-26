#!/bin/sh
gunicorn -w 2 --threads 2 -b 0.0.0.0:8030 server:app --access-logformat '%(t)s "%(r)s" %(s)s %(q)s' --access-logfile -