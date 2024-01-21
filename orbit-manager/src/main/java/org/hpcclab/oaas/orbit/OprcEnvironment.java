package org.hpcclab.oaas.orbit;

public record OprcEnvironment (
  Config config
){
  public record Config (String kafkaBootstrap) {}
}
