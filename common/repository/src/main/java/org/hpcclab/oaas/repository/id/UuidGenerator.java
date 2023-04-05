package org.hpcclab.oaas.repository.id;


import java.util.UUID;

//@ApplicationScoped
public class UuidGenerator implements IdGenerator{

  @Override
  public String generate() {
    return UUID.randomUUID().toString();
  }
}
