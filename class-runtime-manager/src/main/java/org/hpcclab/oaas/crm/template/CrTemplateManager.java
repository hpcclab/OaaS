package org.hpcclab.oaas.crm.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.condition.ConditionProcessor;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;

@ApplicationScoped
@Startup
public class CrTemplateManager {
  private static final Logger logger = LoggerFactory.getLogger(CrTemplateManager.class);
  public static final String DEFAULT = "default";
  final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  final CrmConfig crmConfig;
  final ProtoMapper protoMapper = new ProtoMapperImpl();
  final ConditionProcessor conditionProcessor;
  final EnvironmentManager environmentManager;
  final CrTemplateFactory crTemplateFactory;
  ImmutableMap<String, CrTemplate> templateMap = Maps.immutable.empty();

  @Inject
  public CrTemplateManager(KubernetesClient kubernetesClient,
                           CrmConfig crmConfig,
                           ConditionProcessor conditionProcessor,
                           EnvironmentManager environmentManager, CrTemplateFactory crTemplateFactory) {
    this.crmConfig = crmConfig;
    this.conditionProcessor = conditionProcessor;
    this.environmentManager = environmentManager;
    this.crTemplateFactory = crTemplateFactory;
  }

  public void loadTemplate() {
    try {
      CrtMappingConfig conf;
      var is = getClass().getResourceAsStream("/crts.yml");
      conf = yamlMapper.readValue(is, CrtMappingConfig.class);
      if (conf.templates()==null || conf.templates().isEmpty()) {
        return;
      }
      var op = crmConfig.templateOverride();
      if (op.isPresent()) {
        String templateOverrideString = op.get();
        var override = yamlMapper.readValue(templateOverrideString, CrtMappingConfig.class);
        conf.templates().putAll(override.templates());
      }
      var m = new HashMap<String, CrTemplate>();
      for (var configEntry : conf.templates().entrySet()) {
        var template = crTemplateFactory.create(configEntry.getKey(), configEntry.getValue());
        m.put(configEntry.getKey(), template);
      }
      templateMap = Maps.immutable.ofMap(m);
      if (logger.isInfoEnabled())
        logger.info("Loaded templates {}", templateMap.keysView());
    } catch (IOException e) {
      throw new StdOaasException("Load template error", e);
    }
  }

  public void initTemplates(CrControllerManager controllerManager) {
    for (CrTemplate template : templateMap) {
      template.init(controllerManager, environmentManager);
    }
  }

  public CrTemplate selectTemplate(DeploymentUnit deploymentUnit) {
    var template = deploymentUnit.getCls().getConfig().getCrTemplate();
    if (!template.isEmpty())
      return templateMap.get(template);
    var cls = deploymentUnit.getCls();
    MutableList<CrTemplate> sortedList = templateMap.valuesView()
      .select(tem -> conditionProcessor.matches(tem.getConfig().condition(),
        protoMapper.fromProto(cls)
      ))
      .toSortedList(Comparator.comparing(tem -> tem.getConfig().priority()));
    if (logger.isInfoEnabled())
      logger.info("template candidates for class '{}' are [{}]",
        cls.getKey(), sortedList.collect(crt -> crt.name() + ":" + crt.getConfig().priority()));
    return sortedList
      .getLastOptional()
      .orElseThrow();
  }

  public CrTemplate selectTemplate(ProtoCr protoCr) {
    var templateName = protoCr.getTemplate();
    if (templateName.isEmpty()) templateName = DEFAULT;
    return templateMap.get(templateName);
  }

  public CrController load(OprcEnvironment.Config env, ProtoCr orbit) {
    return selectTemplate(orbit)
      .load(env, orbit);
  }
}
