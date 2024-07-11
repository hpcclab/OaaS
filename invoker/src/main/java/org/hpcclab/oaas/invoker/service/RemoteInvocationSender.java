package org.hpcclab.oaas.invoker.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;

import java.util.function.Supplier;

/**
 * @author Pawissanutt
 */
public interface RemoteInvocationSender {
  Uni<ProtoInvocationResponse> send(Supplier<CrHash.ApiAddress> addrSupplier,
                                    ProtoInvocationRequest request);
}
