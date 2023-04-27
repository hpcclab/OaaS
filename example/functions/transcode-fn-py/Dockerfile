FROM alpine
#FROM jrottenberg/ffmpeg:4.4-alpine
RUN apk add --update --no-cache python3 curl ffmpeg && \
    ln -sf python3 /usr/bin/python &&\
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
