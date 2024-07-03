package org.hpcclab.oaas.crm.filter;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class K8sFilterFactory implements FilterFactory<List<HasMetadata>> {
  private static final Logger logger = LoggerFactory.getLogger( K8sFilterFactory.class );
  @Override
  public CrFilter<List<HasMetadata>> create(CrtMappingConfig.FilterConfig filter) {
    logger.debug("create filter {}", filter);
    return switch (filter.type()) {
      case TolerationInjectingFilter.NAME -> new TolerationInjectingFilter(filter.conf());
      case PodAffinityInjectingFilter.NAME -> new PodAffinityInjectingFilter(filter.conf());
      case NodeAffinityInjectingFilter.NAME -> new NodeAffinityInjectingFilter(filter.conf());
      default -> throw new IllegalStateException("Unexpected value: " + filter.type());
    };
  }
}
