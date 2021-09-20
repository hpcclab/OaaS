#!/bin/sh

kubectl port-forward -n msc svc/msc-rmq 5672:5672

export CI_REGISTRY_IMAGE=registry.gitlab.com/multimedia-streaming-cloud-ecosystem/msc-oaas
export JAVA_HOME=~/.jdks/openjdk-16.0.2/

./mvnw package -DskipTests\
    -Dquarkus.container-image.username=gitlab+deploy-token-567770 \
    -Dquarkus.container-image.password=u7pC_JXyVe7g69vxoht8 \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true


export CI_REGISTRY_IMAGE=core.harbor.OAACluster01/oaas
export JAVA_HOME=~/.jdks/openjdk-17/

./mvnw package -DskipTests\
    -Dquarkus.container-image.username=hpcclab \
    -Dquarkus.container-image.password=Hpcclab123 \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true \
    -Dquarkus.jib.always-cache-base-image=true \
    -Dquarkus.container-image.insecure=true

$ENV:JAVA_HOME="C:\Program Files\Java\jdk-17"
$ENV:CI_REGISTRY_IMAGE="core.harbor.OAACluster01/oaas"
mvn package -DskipTests "-Dquarkus.container-image.username=hpcclab"    "-Dquarkus.container-image.password=Hpcclab123"    "-Dquarkus.container-image.build=true"  "-Dquarkus.container-image.push=true"    "-Dquarkus.container-image.insecure=true" "-Dquarkus.jib.base-jvm-image=core.harbor.OAACluster01/proxy/adoptopenjdk/openjdk16:ubi-minimal-jre"

NAMESPACE="msc"
kubectl -n $NAMESPACE apply -f kafka -f ksql
kubectl -n $NAMESPACE apply -k oaas/dev

kubectl exec -i -t -n msc ksqldb-cli-579bfbd6b6-lc7lw -c ksqldb-cli "--" sh -c "ksql http://ksqldb-server"

kubectl -n $NAMESPACE delete -k oaas/dev
kubectl -n $NAMESPACE delete -f kafka -f ksql
kubectl -n $NAMESPACE delete $(kubectl -n $NAMESPACE get kafkatopic -o name)
