FROM python:3.11-buster

RUN apt update -y && \
    apt install -y ffmpeg && \
    python3 -m ensurepip && \
    mkdir -p "/opt/app"

WORKDIR /opt/app
COPY requirements.txt ./
RUN pip3 install -r requirements.txt
COPY main.py config.py ./

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c"]
#CMD ["python3 main.py"]
CMD ["gunicorn -c config.py -k uvicorn.workers.UvicornWorker main:app"]
