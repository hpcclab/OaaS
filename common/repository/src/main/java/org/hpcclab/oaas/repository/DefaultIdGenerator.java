package org.hpcclab.oaas.repository;


import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class DefaultIdGenerator implements IdGenerator{

  @Override
  public String generate() {
    return UUID.randomUUID().toString();
  }
}
