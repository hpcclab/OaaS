package org.hpcclab.oaas.crm.filter;

import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.controller.Filterable;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface FilterFactory<T> {
  CrFilter<T> create(CrtMappingConfig.FilterConfig filter);

  default void injectFilter(List<CrtMappingConfig.FilterConfig> filterConfs,
                            Filterable<T> filterable) {
    if (filterConfs!=null) {
      for (CrtMappingConfig.FilterConfig filterConf : filterConfs) {
        CrFilter<T> filter = create(filterConf);
        filterable.addFilter(filter);
      }
    }
  }
}
