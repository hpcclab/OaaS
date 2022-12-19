package org.hpcclab.oaas.repository;


import com.github.f4b6a3.tsid.TsidCreator;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class TsidGenerator implements IdGenerator{

  @Override
  public String generate() {
    return TsidCreator.getTsid1024().toString();
  }
}
