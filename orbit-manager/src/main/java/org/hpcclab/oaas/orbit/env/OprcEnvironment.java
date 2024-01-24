package org.hpcclab.oaas.orbit.env;

public record OprcEnvironment (
  Config config,
  EnvResource total,
  EnvResource usable
){
  public record Config (String kafkaBootstrap, String classManagerHost, String classManagerPort) {}
}
