mvn := "mvnd"
export CI_REGISTRY_IMAGE := "ghcr.io/hpcclab/oaas"

build options="":
  ./mvnw  package {{options}}

build-no-test options="":
  {{mvn}} package -DskipTests {{options}}

build-image : (build-no-test '"-Dquarkus.container-image.build=true"')

kind-upload-image:
  docker images --format json | jq -r .Repository | grep oaas- | xargs kind load docker-image -n 1node-cluster

k3d-upload-image:
  docker images --format json | jq -r .Repository | grep oaas- | xargs k3d image import

k3d-build-image: build-no-test && k3d-upload-image
  docker compose build

k3d-deploy: k8s-deploy-deps
  kubectl apply -n oaas -k deploy/oaas/local-build
  kubectl apply -n oaas -f deploy/local-k8s/oaas-ingress.yml

k8s-deploy-preq:
  kubectl create ns oaas
  kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.10.2/serving-crds.yaml
  kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.10.2/serving-core.yaml
  kubectl apply -f https://github.com/knative/net-kourier/releases/download/knative-v1.10.0/kourier.yaml
  kubectl patch configmap/config-network \
    --namespace knative-serving \
    --type merge \
    --patch '{"data":{"ingress-class":"kourier.ingress.networking.knative.dev"}}'

  helm install oaas oci://registry-1.docker.io/bitnamicharts/kafka -n oaas
  kubectl apply -n oaas -f deploy/local-k8s/kafka-ui.yml

k8s-deploy-deps:
  kubectl apply -n oaas -f deploy/local-k8s/minio.yml

  kubectl apply -n oaas -f deploy/arango/arango-single.yml
  kubectl apply -n oaas -f deploy/local-k8s/arango-ingress.yml


k8s-clean:
  kubectl delete -n oaas ksvc -l oaas.function
  kubectl delete -n oaas -k deploy/oaas/local-build
  kubectl delete -n oaas -f deploy/local-k8s/oaas-ingress.yml

  kubectl delete -n oaas -f deploy/arango/arango-single.yml
  kubectl delete -n oaas -f deploy/local-k8s/arango-ingress.yml

  kubectl delete -n oaas -f deploy/local-k8s/minio.yml

k3d-create:
  K3D_FIX_DNS=1 k3d cluster create -p "9090:80@loadbalancer"

compose-build-up: build-no-test
  docker compose up -d --build

compose-clean:
  docker compose down -v
