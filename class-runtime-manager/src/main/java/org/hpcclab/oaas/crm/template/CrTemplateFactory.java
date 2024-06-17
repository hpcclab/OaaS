package org.hpcclab.oaas.crm.template;

import org.hpcclab.oaas.crm.CrtMappingConfig;

/**
 * @author Pawissanutt
 */
public interface CrTemplateFactory {
  CrTemplate create(String name, CrtMappingConfig.CrtConfig config);
}
