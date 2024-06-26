package org.hpcclab.oaas.invocation.task;

import io.vertx.mutiny.core.buffer.Buffer;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;

/**
 * @author Pawissanutt
 */
public interface TaskEncoder {
  default Buffer encodeTask(InvokingDetail<?> invokingDetail){
    return encodeTask((OTask) invokingDetail.getContent());
  }
  Buffer encodeTask(OTask task);
  OTask decodeTask(Buffer buffer);
  Buffer encodeCompletion(OTaskCompletion completion);
  OTaskCompletion decodeCompletion(Buffer buffer);
}
