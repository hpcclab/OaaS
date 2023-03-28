package org.hpcclab.oaas.repository;

import com.github.f4b6a3.tsid.TsidFactory;
import jakarta.enterprise.context.ApplicationScoped;

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
