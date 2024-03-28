package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;

import java.util.List;

public record FnResourcePlan(
  List<HasMetadata> resources,
  List<OFunctionStatusUpdate> fnUpdates) {

  public static final FnResourcePlan EMPTY = new FnResourcePlan(List.of(), List.of());
}
