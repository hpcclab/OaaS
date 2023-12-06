package org.hpcclab.oaas.controller.rpc;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.proto.InternalClassService;
import org.hpcclab.oaas.proto.ProtoOClass;

public class InternalClassRpc implements InternalClassService {
  @Override
  public Uni<ProtoOClass> update(ProtoOClass request) {
    return null;
  }
}
