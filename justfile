build:
  ./mvnw package

build-no-test:
  mvnd package -DskipTests

kind-upload-image:
  docker images --format json | jq -r .Repository | grep oaas- | xargs kind load docker-image -n 1node-cluster

k3d-upload-image:
  docker images --format json | jq -r .Repository | grep oaas- | xargs k3d image import

k3d-build-image: k3d-upload-image

k3d-deploy:
  kubectl apply -n oaas -k deploy/oaas/local-build
