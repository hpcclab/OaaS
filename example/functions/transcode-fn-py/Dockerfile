#FROM python:3.11-buster
FROM python:3.11-slim

RUN apt update -y && \
    apt install -y ffmpeg && \
    python3 -m ensurepip && \
    mkdir -p "/opt/app" && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

WORKDIR /opt/app
COPY requirements.txt ./
RUN pip3 install -r requirements.txt
COPY main.py config.py ./

EXPOSE 8080
CMD ["hypercorn", "-c" , "file:config.py","main:app"]
