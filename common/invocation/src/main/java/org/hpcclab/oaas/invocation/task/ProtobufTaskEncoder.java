package org.hpcclab.oaas.invocation.task;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.mutiny.core.buffer.Buffer;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;
import org.hpcclab.oaas.proto.ProtoOTask;
import org.hpcclab.oaas.proto.ProtoOTaskCompletion;

/**
 * @author Pawissanutt
 */
public class ProtobufTaskEncoder implements TaskEncoder {
  ProtoMapper protoMapper = ProtoMapper.INSTANCE;
  @Override
  public Buffer encodeTask(OTask task) {
    return Buffer.buffer(protoMapper.toProto(task).toByteArray());
  }

  @Override
  public OTask decodeTask(Buffer buffer) {
    try {
      return protoMapper.fromProto(ProtoOTask.parseFrom(buffer.getBytes()));
    } catch (InvalidProtocolBufferException e) {
      throw new StdOaasException(e);
    }
  }

  @Override
  public Buffer encodeCompletion(OTaskCompletion completion) {
    return Buffer.buffer(protoMapper.toProto(completion).toByteArray());
  }

  @Override
  public OTaskCompletion decodeCompletion(Buffer buffer) {
    if (buffer==null) {
      return new OTaskCompletion()
        .setErrorMsg("No task completion");
    }
    try {
      return protoMapper.fromProto(ProtoOTaskCompletion.parseFrom(buffer.getBytes()));
    } catch (InvalidProtocolBufferException e) {
      return new OTaskCompletion()
        .setErrorMsg(
          "Can not parse the task completion message. [%s]"
            .formatted(e.getMessage()));
    }
  }
}
