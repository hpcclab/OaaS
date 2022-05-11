tag=0.1.2
base=core.harbor.10.131.36.2.nip.io/oaas

docker build -t $base/add-vfilter-function:$tag .
docker push $base/add-vfilter-function:$tag
