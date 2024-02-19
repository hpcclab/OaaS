mvn := "mvnd"
#mvn := "./mvnw"
#export CI_REGISTRY_IMAGE := "ghcr.io/hpcclab/oaas"
export CI_REGISTRY_IMAGE := "ghcr.io/pawissanutt/oaas"
#export QUARKUS_DOCKER_EXECUTABLE_NAME := "docker"

build options="":
  ./mvnw  package {{options}}

build-no-test options="":
  {{mvn}} package -DskipTests {{options}}

build-image-native options="":
  ./mvnw package -DskipTests -Pnative  "-Dquarkus.container-image.build=true" "-Dquarkus.native.remote-container-build=true" {{options}}

build-native-window:
    pwsh -c "cmd /c 'call \"C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat\" && mvn package -DskipTests -Dnative'"


build-image : (build-no-test '"-Dquarkus.container-image.build=true"')

build-image-push : (build-no-test '"-Dquarkus.container-image.build=true" "-Dquarkus.container-image.push=true"')

build-image-docker : build-no-test
  docker compose build

k3d-build-image: build-image
  docker images --format json | jq -r .Repository | grep ghcr.io/hpcclab/oaas | grep -v fn-py | xargs k3d image import

k8s-deploy: k8s-deploy-deps
  kubectl apply -n oaas -k deploy/oaas/base
  kubectl apply -n oaas -f deploy/local-k8s/oaas-ingress.yml
  kubectl apply -n oaas -f deploy/local-k8s/invoker-np.yml

k3d-reload: k3d-build-image
  kubectl -n oaas rollout restart deployment -l platform=oaas
  kubectl -n oaas rollout restart deployment -l cr-part=invoker
  kubectl -n oaas rollout restart deployment -l cr-part=storage-adapter

rd-reload: build-image-docker
  kubectl -n oaas rollout restart deployment -l platform=oaas
  kubectl -n oaas rollout restart deployment -l cr-part=invoker
  kubectl -n oaas rollout restart deployment -l cr-part=storage-adapter

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

k8s-deploy-deps:
  kubectl apply -n oaas -f deploy/local-k8s/kafka-cluster.yml
  kubectl apply -n oaas -f deploy/local-k8s/kafka-ui.yml
  kubectl apply -n oaas -f deploy/local-k8s/minio.yml
  kubectl apply -n oaas -f deploy/arango/arango-single.yml
  kubectl apply -n oaas -f deploy/local-k8s/arango-ingress.yml


k8s-clean:
  kubectl delete -n oaas ksvc -l oaas.function
  kubectl delete -n oaas -k deploy/oaas/base
  kubectl delete -n oaas -f deploy/local-k8s/oaas-ingress.yml

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
