package org.hpcclab.oaas.invocation.task;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;
import org.hpcclab.oaas.proto.FunctionExecutor;
import org.hpcclab.oaas.proto.ProtoOTask;
import org.hpcclab.oaas.proto.ProtoOTaskCompletion;

/**
 * @author Pawissanutt
 */
public class GrpcOffloader implements OffLoader{
  final FunctionExecutor executor;
  ProtoMapper protoMapper = ProtoMapper.INSTANCE;

  public GrpcOffloader(FunctionExecutor executor) {
    this.executor = executor;
  }

  @Override
  public Uni<OTaskCompletion> offload(InvokingDetail<?> invokingDetail) {
    OTask content = (OTask) invokingDetail.getContent();
    ProtoOTask proto = protoMapper.toProto(content);
    var smtTs = System.currentTimeMillis();
    Uni<ProtoOTaskCompletion> invoke = executor.invoke(proto);
    return invoke
      .map(protoMapper::fromProto)
      .map(taskCompletion -> taskCompletion.setId(invokingDetail.getId())
        .setSmtTs(smtTs)
        .setCptTs(System.currentTimeMillis())
      );
  }
}
