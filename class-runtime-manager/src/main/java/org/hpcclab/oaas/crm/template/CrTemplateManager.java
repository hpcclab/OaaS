package org.hpcclab.oaas.crm.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.optimize.DefaultQoSOptimizer;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

@ApplicationScoped
@Startup
public class CrTemplateManager {
  final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  final KubernetesClient kubernetesClient;
  final DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;
  final CrmConfig crmConfig;
  ImmutableMap<String, ClassRuntimeTemplate> templateMap = Maps.immutable.empty();
  public static final String DEFAULT = "default";
  @Inject
  public CrTemplateManager(KubernetesClient kubernetesClient,
                           @GrpcClient("package-manager")
                           DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater,
                           CrmConfig crmConfig) {
    this.kubernetesClient = kubernetesClient;
    this.crmConfig = crmConfig;
    Objects.requireNonNull(deploymentStatusUpdater);
    this.deploymentStatusUpdater = deploymentStatusUpdater;
      try {
          loadTemplate();
      } catch (IOException e) {
          throw new StdOaasException("Load template error",e);
      }
  }

  public void loadTemplate() throws IOException {
    CrtMappingConfig conf;
    var file = "/crts.yaml";
    var is = getClass().getResourceAsStream(file);
    conf = yamlMapper.readValue(is, CrtMappingConfig.class);
    if (conf.templates()==null || conf.templates().isEmpty()) {
      return;
    }
    var op = crmConfig.templateOverride();
    if (op.isPresent()) {
      String templateOverrideString = op.get();
      var override =yamlMapper.readValue(templateOverrideString, CrtMappingConfig.class);
      conf.templates().putAll(override.templates());
    }
    var m = new HashMap<String, ClassRuntimeTemplate>();
    for (var configEntry : conf.templates().entrySet()) {
      var template = createCrt(configEntry.getValue());
      template.init();
      m.put(configEntry.getKey(), template);
    }
    templateMap = Maps.immutable.ofMap(m);
  }

  private ClassRuntimeTemplate createCrt(CrtMappingConfig.CrtConfig config) {
    if (config.type().equals(DEFAULT)) {
      return new DefaultCrTemplate(
        kubernetesClient,
        selectOptimizer(config),
        config,
        deploymentStatusUpdater
      );
    } else {
      throw new StdOaasException("No available CR template with type " + config.type());
    }
  }

  public QosOptimizer selectOptimizer(CrtMappingConfig.CrtConfig config) {
    return new DefaultQoSOptimizer(config);
  }

  public ClassRuntimeTemplate selectTemplate(OprcEnvironment env,
                                             DeploymentUnit deploymentUnit) {
    var template = deploymentUnit.getCls().getConfig().getCrTemplate();
    if (template.isEmpty()) template = DEFAULT;
    return templateMap.get(template);
  }

  public ClassRuntimeTemplate selectTemplate(ProtoCr protoCr) {
    var template = protoCr.getType();
    if (template.isEmpty()) template = DEFAULT;
    return templateMap.get(template);
  }

  public CrController load(OprcEnvironment env, ProtoCr orbit) {
    return selectTemplate(orbit)
      .load(env, orbit);
  }
}
