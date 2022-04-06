tag=0.2.16
base=core.harbor.10.131.36.2.nip.io/oaas

docker build -t $base/transcode-function:$tag .
docker push $base/transcode-function:$tag
