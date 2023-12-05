package org.hpcclab.oaas.provisioner.provisioner;

import org.hpcclab.oaas.provisioner.OrbitTemplate;

import java.util.function.Consumer;

public class OrbitProvisioner implements Provisioner<OrbitTemplate>{
  @Override
  public Consumer<OrbitTemplate> provision(OrbitTemplate functionRecord) {
    return null;
  }
}
