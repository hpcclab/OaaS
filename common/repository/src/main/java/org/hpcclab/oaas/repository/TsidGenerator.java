package org.hpcclab.oaas.repository;


import com.github.f4b6a3.tsid.TsidCreator;
import com.github.f4b6a3.tsid.TsidFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class TsidGenerator implements IdGenerator{

  TsidFactory tsidFactory;

  public TsidGenerator() {
    tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public String generate() {
    return tsidFactory.create().toString();
  }
}
