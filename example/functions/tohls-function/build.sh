tag=0.1.12
base=core.harbor.10.131.36.2.nip.io/oaas

docker build -t $base/tohls-function:$tag .
docker push $base/tohls-function:$tag
