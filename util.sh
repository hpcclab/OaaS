#!/bin/sh

export CI_REGISTRY_IMAGE=core.harbor.10.131.36.27.nip.io/oaas
export JAVA_HOME=~/.jdks/temurin-17/

./mvnw package -DskipTests\
    -Dquarkus.container-image.username=admin \
    -Dquarkus.container-image.password=Harbor12345 \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true \
    -Dquarkus.jib.always-cache-base-image=true \
    -Dquarkus.jib.base-jvm-image=core.harbor.10.131.36.27.nip.io/proxy/library/eclipse-temurin:17-focal \
    -Dquarkus.container-image.insecure=true

$ENV:JAVA_HOME="$HOME/.jdks/temurin-17.0.1"
$ENV:CI_REGISTRY_IMAGE="core.harbor.10.131.36.27.nip.io/oaas"
$ENV:TAG=":kvstore"
./mvnw package "-Dquarkus.container-image.username=admin"    "-Dquarkus.container-image.password=Harbor12345"    "-Dquarkus.container-image.build=true"  "-Dquarkus.container-image.push=true"    "-Dquarkus.container-image.insecure=true" "-Dquarkus.jib.base-jvm-image=core.harbor.10.131.36.27.nip.io/proxy/library/eclipse-temurin:17-focal" -DskipTests


NAMESPACE="msc"
kubectl -n $NAMESPACE apply -f kafka
kubectl -n $NAMESPACE apply -f knative/broker.yml
kubectl -n $NAMESPACE apply -k oaas/kafka-related
kubectl -n $NAMESPACE apply -k oaas/dev

kubectl exec -n msc  deployment/ksqldb-cli -ti -- bash -c "ksql http://ksqldb-server"

kubectl -n $NAMESPACE delete -k oaas/dev
kubectl -n $NAMESPACE delete -f kafka
kubectl -n $NAMESPACE delete -k oaas/kafka-related
kubectl -n $NAMESPACE delete $(kubectl -n $NAMESPACE get kafkatopic -o name)


echo "-----BEGIN CERTIFICATE-----
MIIDFDCCAfygAwIBAgIRAMlV1L7uI1dgTgcF+1B3RjEwDQYJKoZIhvcNAQELBQAw
FDESMBAGA1UEAxMJaGFyYm9yLWNhMB4XDTIxMTExNTA4MjU0NVoXDTIyMTExNTA4
MjU0NVowFDESMBAGA1UEAxMJaGFyYm9yLWNhMIIBIjANBgkqhkiG9w0BAQEFAAOC
AQ8AMIIBCgKCAQEAuxQMOiH/5JQWJ8QBo6UTvMRcKNYQoqU1hs3dNpyjhuE+x9jV
ytNQIutQZuIX9yi1N77OdB+qXtQpq0O6u5G91zqLFPFrLp78OUWUePEneZD3hWpv
YYw6uz74/2gSF/6ySu4tzLm0nhg1W4299fz12/798R89flQOqYGyGGUS0vAfRPHn
tcComj0QQoyOcMIa9/QvJaO9DOPWKWUGcaGtHdKz7RDD/S8K+zZTTo9L9yAFzzCU
/kVT7TZSY8hHkc9KyUK+Y+RxO3MI20O8I5fpkqw/sxgLkXHSdmFDBkLh5Kt8MsN2
4dLm5IoMe85DrHqacPheM4Y7RSVHRM2GFxkoiwIDAQABo2EwXzAOBgNVHQ8BAf8E
BAMCAqQwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA8GA1UdEwEB/wQF
MAMBAf8wHQYDVR0OBBYEFPRQ0+D0I7O6Z69n8tZfoRhClnTIMA0GCSqGSIb3DQEB
CwUAA4IBAQB8O3+IcbCPFx2RVWNTF6a9iP4b3JUedcNV4CQjlWGQiPvCsbzsLyah
GBShCQtMAep96De6DGrQWSMq3noeEPUy3uQkg2/KyGXwz4jrgMEGJEmgqjE7aMNd
5I3743RACYrn5Wb+cbc7JTQ5pnYC8PJDXiEJHAdbC0HAPiZJRpENtZj4DgCw/cz4
E1HrDBABu7u6tjYUCInI2pJK1UkMBU3YQkm4PFwavZ+CnotrwriRwmReY9Uw4rrj
TwZ+8ii1gCxrLydQ9IEVYP5D0KxK3EO5Q/afH9VkLKeL5e3vhePCKR6oO5gpiukb
udxZ23AAL2CFKkdzlEWAwY3b4W3d24VD
-----END CERTIFICATE-----" > /usr/local/share/ca-certificates/harbor.crt
update-ca-certificates


$ENV:JAVA_HOME="$HOME/.jdks/temurin-17.0.1"
$ENV:CI_REGISTRY_IMAGE="core.harbor.10.131.36.27.nip.io/oaas"
$ENV:TAG=":native"
./mvnw package -Pnative "-Dquarkus.native.container-build=true" "-Dquarkus.native.container-runtime=docker" "-Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java17" "-Dquarkus.container-image.build=true" "-Dquarkus.container-image.username=admin" "-Dquarkus.container-image.password=Harbor12345"  "-Dquarkus.container-image.push=true" "-Dquarkus.container-image.insecure=true"
