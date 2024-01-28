mvn := "mvnd"
#mvn := "./mvnw"
export CI_REGISTRY_IMAGE := "ghcr.io/hpcclab/oaas"

build options="":
  ./mvnw  package {{options}}

build-no-test options="":
  {{mvn}} package -DskipTests {{options}}

build-image : (build-no-test '"-Dquarkus.container-image.build=true"')

kind-build-image: build-image
  docker images --format json | jq -r .Repository | grep ghcr.io/hpcclab/oaas | xargs kind load docker-image -n 1node-cluster

k3d-build-image: build-image
  docker images --format json | jq -r .Repository | grep ghcr.io/hpcclab/oaas | grep -v fn-py | xargs k3d image import

k3d-deploy: k8s-deploy-deps
  kubectl apply -n oaas -k deploy/oaas/orbit
  kubectl apply -n oaas -f deploy/local-k8s/orbit-ingress.yml

k3d-reload: k3d-build-image
  kubectl -n oaas delete pod -l platform=oaas

rd-reload: build-image
  kubectl -n oaas delete pod -l platform=oaas
  kubectl -n oaas delete pod -l orbit-part=invoker
  kubectl -n oaas delete pod -l orbit-part=storage-adapter

k8s-deploy-preq kn-version="v1.12.3" kourier-version="v1.12.3":
  kubectl create namespace oaas --dry-run=client -o yaml | kubectl apply -f -
  kubectl apply -f https://github.com/knative/serving/releases/download/knative-{{kn-version}}/serving-crds.yaml
  kubectl apply -f https://github.com/knative/serving/releases/download/knative-{{kn-version}}/serving-core.yaml
  kubectl apply -f https://github.com/knative/net-kourier/releases/download/knative-{{kourier-version}}/kourier.yaml
  kubectl patch configmap/config-network \
    --namespace knative-serving \
    --type merge \
    --patch '{"data":{"ingress-class":"kourier.ingress.networking.knative.dev"}}'

  kubectl apply -f 'https://strimzi.io/install/latest?namespace=oaas' -n oaas
  kubectl apply -n oaas -f deploy/local-k8s/kafka-cluster.yml
  kubectl apply -n oaas -f deploy/local-k8s/kafka-ui.yml

k8s-deploy-deps:
  kubectl apply -n oaas -f deploy/local-k8s/minio.yml
  kubectl apply -n oaas -f deploy/arango/arango-single.yml
  kubectl apply -n oaas -f deploy/local-k8s/arango-ingress.yml


k8s-clean:
  kubectl delete -n oaas ksvc -l oaas.function
  kubectl delete -n oaas -k deploy/oaas/orbit
  kubectl delete -n oaas -f deploy/local-k8s/orbit-ingress.yml

  kubectl delete -n oaas -f deploy/arango/arango-single.yml
  kubectl delete -n oaas -f deploy/local-k8s/arango-ingress.yml

  kubectl delete -n oaas -f deploy/local-k8s/minio.yml

  kubectl delete -n oaas -f deploy/local-k8s/kafka-ui.yml
  kubectl delete -n oaas -f deploy/local-k8s/kafka-cluster.yml

k3d-create:
  K3D_FIX_DNS=1 k3d cluster create -p "9090:80@loadbalancer"

compose-build-up: build-image
  docker compose up -d

compose-down:
  docker compose down -v
