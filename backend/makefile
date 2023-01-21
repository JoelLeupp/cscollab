server:
	gunicorn -w 2 -b localhost:8030 server:app --access-logformat '%(t)s "%(r)s" %(s)s %(q)s' --access-logfile -
