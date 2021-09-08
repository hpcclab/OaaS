#!/bin/sh

kubectl port-forward -n msc svc/msc-rmq 5672:5672

export CI_REGISTRY_IMAGE=registry.gitlab.com/multimedia-streaming-cloud-ecosystem/msc-object-controller
export JAVA_HOME=~/.jdks/openjdk-16.0.2/

./mvnw package -DskipTests\
    -Dquarkus.container-image.username=gitlab+deploy-token-567770 \
    -Dquarkus.container-image.password=u7pC_JXyVe7g69vxoht8 \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true
