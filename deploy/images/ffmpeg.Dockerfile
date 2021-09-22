FROM jrottenberg/ffmpeg:4.4-alpine
RUN apk add curl

# docker build -f ffmpeg.Dockerfile -t core.harbor.10.131.36.27.nip.io/oaas/ffmpeg .
# docker push core.harbor.10.131.36.27.nip.io/oaas/ffmpeg
