#FROM python:3.11-buster
FROM python:3.11-slim

RUN mkdir -p "/opt/app"
WORKDIR /opt/app
COPY requirements.txt ./
RUN apt update \
    && apt install -y ffmpeg libsm6 libxext6  \
    && apt clean \
    && pip3 install -r requirements.txt \
    && printf 'from ultralytics import YOLO\nYOLO("yolov8m.pt")' |python3
COPY main.py config.py ./

EXPOSE 8080
CMD ["gunicorn", "-c" , "config.py", "-k",  "uvicorn.workers.UvicornWorker" ,"main:app"]
