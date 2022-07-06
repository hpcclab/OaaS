tag=0.3.6
base=core.harbor.10.131.36.2.nip.io/oaas
name=transcode-function
image=$base$name:$tag

docker build -t $image .
docker push $image
