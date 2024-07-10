package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.filter.CrFilter;
import org.hpcclab.oaas.proto.ProtoFunctionType;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pawissanutt
 */
public class UnifyFnCrControllerFactory implements FnCrControllerFactory<HasMetadata> {

  final CrtMappingConfig.FnConfig fnConfig;
  final OprcEnvironment.Config envConfig;
  List<CrFilter<List<HasMetadata>>> filters = new ArrayList<>();

  public UnifyFnCrControllerFactory(CrtMappingConfig.FnConfig fnConfig,
                                    OprcEnvironment.Config envConfig) {
    this.fnConfig = fnConfig;
    this.envConfig = envConfig;
  }

  @Override
  public FnCrComponentController<HasMetadata> create(ProtoOFunction function) {
    if (function.getType()==ProtoFunctionType.PROTO_FUNCTION_TYPE_MACRO)
      return new FnCrComponentController.NoOp<>();
    if (function.getType()==ProtoFunctionType.PROTO_FUNCTION_TYPE_BUILTIN)
      return new FnCrComponentController.NoOp<>();
    if (!function.getProvision().getDeployment().getImage().isEmpty()) {
      var controller = new DeploymentFnCrComponentController(
        fnConfig, envConfig, function);
      filters.forEach(controller::addFilter);
      return controller;
    } else if (!function.getProvision().getKnative().getImage().isEmpty()) {
      var controller = new KnativeFnCrComponentController(
        fnConfig, envConfig, function);
      filters.forEach(controller::addFilter);
      return controller;
    }
    throw new CrDeployException("Can not find suitable functions controller for functions:\n" + function);
  }

  @Override
  public void addFilter(CrFilter<List<HasMetadata>> filter) {
    filters.add(filter);
  }
}
