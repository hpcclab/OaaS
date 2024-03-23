import os

bind = '0.0.0.0:' + os.environ.get('PORT', '8080')
workers = int(os.environ.get('WORKERS', '4'))
access_log_format = '%(h)s "%(r)s" %(s)s %(b)s "%(f)s" %(D)s µs'
accesslog = '-'
