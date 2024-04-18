FROM python:3.11-slim

RUN mkdir -p "/opt/app"
WORKDIR /opt/app
COPY requirements.txt ./
RUN pip3 install --no-cache-dir --upgrade -r requirements.txt
COPY main.py config.py ./

EXPOSE 8080
CMD ["gunicorn", "-c" , "config.py", "-k",  "uvicorn.workers.UvicornWorker" ,"main:app"]

