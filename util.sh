#!/bin/sh

export CI_REGISTRY_IMAGE=core.harbor.10.131.36.27.nip.io/oaas
export JAVA_HOME=~/.jdks/openjdk-17/

./mvnw package -DskipTests\
    -Dquarkus.container-image.username=admin \
    -Dquarkus.container-image.password=Harbor12345 \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true \
    -Dquarkus.jib.always-cache-base-image=true \
    -Dquarkus.jib.base-jvm-image=core.harbor.10.131.36.27.nip.io/proxy/adoptopenjdk/openjdk16:ubi-minimal-jre \
    -Dquarkus.container-image.insecure=true

$ENV:JAVA_HOME="C:\Program Files\Java\jdk-17"
$ENV:CI_REGISTRY_IMAGE="core.harbor.10.131.36.27.nip.io/oaas"
mvn package -DskipTests "-Dquarkus.container-image.username=admin"    "-Dquarkus.container-image.password=Harbor12345"    "-Dquarkus.container-image.build=true"  "-Dquarkus.container-image.push=true"    "-Dquarkus.container-image.insecure=true" "-Dquarkus.jib.base-jvm-image=core.harbor.10.131.36.27.nip.io/proxy/adoptopenjdk/openjdk16:ubi-minimal-jre"

NAMESPACE="msc"
kubectl -n $NAMESPACE apply -f kafka -f ksql
kubectl -n $NAMESPACE apply -k oaas/dev

kubectl exec -n msc  deployment/ksqldb-cli -ti -- bash -c "ksql http://ksqldb-server"

kubectl -n $NAMESPACE delete -k oaas/dev
kubectl -n $NAMESPACE delete -f kafka -f ksql
kubectl -n $NAMESPACE delete $(kubectl -n $NAMESPACE get kafkatopic -o name)


echo "-----BEGIN CERTIFICATE-----
MIIDFDCCAfygAwIBAgIRALK1xK9A8YVswTfBTiGYBnEwDQYJKoZIhvcNAQELBQAw
FDESMBAGA1UEAxMJaGFyYm9yLWNhMB4XDTIxMDkyMTA0NDUwN1oXDTIyMDkyMTA0
NDUwN1owFDESMBAGA1UEAxMJaGFyYm9yLWNhMIIBIjANBgkqhkiG9w0BAQEFAAOC
AQ8AMIIBCgKCAQEAqfrHiLqLrhNDCDAZVO2cIUs6tLHgYo4qpunYCORqESSKkCCC
PEuqflpvYWweZ6xcu4O0k8gmHM54+MOWCr8zHMqWsJDbIXX/hEISJy/LvVZeNQQb
qnmjrjy1PeH1M+d2v2zLvl0JRWUzUvdDp2ZuekPBsZJzmsZQKC1eYoGV0qhh1bTm
V6pzsDh9wvXG4I/lNqUMUCDaEYRAe3TxILoHZ4LXsB5hUwZUdX5ziZAmEfUXTffX
WAUE5pqDiVCXrAUdDFE1oO7uFZB/mLIt+zmDwokbLLJpmFrs2863KK0tetmdudGO
bzNH7VmXotBRM7q/ZLRnWBcjnFZmpMRUbl5KZQIDAQABo2EwXzAOBgNVHQ8BAf8E
BAMCAqQwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA8GA1UdEwEB/wQF
MAMBAf8wHQYDVR0OBBYEFLJPYt/O8ZPiSXN0ma4ErrvxaMJBMA0GCSqGSIb3DQEB
CwUAA4IBAQAimCXKCvGvQFY4npuXm9Y+2ToRDzVnW1LdVypCrpZhxWT7iGW42BFh
+YMXhqweWVR2cqqM7J5wvI1R0w4Dic2voCZPEkVkYJqLTl8QQyrsgE76m9ICyFtp
LsThHVlddZQcn5gLA5bcbh7g/OLEUcWlkWckzHQxq6uohNYl4oue5vn0u4OGVStT
QfxIg8tAZoOeK0dahypBiSvEruv9plH72xKvgKqsUUniqzD/6tLsI2nto/o3bvLq
Zs4Q6fQxS/DPh2m+kYOcWuXEvYsPp/gdbhMggislytlPjX90HAJJqaUPqdrCms6q
h/8AnOrY1IJCwkMNFsARJhwLlDdvbQGQ
-----END CERTIFICATE-----" > /usr/local/share/ca-certificates/harbor.crt
update-ca-certificates
