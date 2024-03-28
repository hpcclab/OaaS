import multiprocessing
from os import environ

bind = '0.0.0.0:' + environ.get('PORT', '8080')
workers = int(environ.get('WORKERS', '4'))
access_log_format = '%(h)s %(l)s %(u)s %(t)s "%(r)s" %(s)s %(b)s "%(f)s" "%(a)s" %(D)s ms'
accesslog = '-'
