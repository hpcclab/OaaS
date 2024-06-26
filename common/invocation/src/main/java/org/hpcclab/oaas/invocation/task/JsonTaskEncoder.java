package org.hpcclab.oaas.invocation.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.mutiny.core.buffer.Buffer;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class JsonTaskEncoder implements TaskEncoder {
  ObjectMapper mapper;

  public JsonTaskEncoder(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Buffer encodeTask(OTask task) {
    try {
      return Buffer.buffer(mapper.writeValueAsBytes(task));
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e);
    }
  }

  @Override
  public OTask decodeTask(Buffer buffer) {
    try {
      return mapper.readValue(buffer.getBytes(), OTask.class);
    } catch (IOException e) {
      throw new StdOaasException(e);
    }
  }

  @Override
  public Buffer encodeCompletion(OTaskCompletion completion) {
    try {
      return Buffer.buffer(mapper.writeValueAsBytes(completion));
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e);
    }
  }

  @Override
  public OTaskCompletion decodeCompletion(Buffer buffer) {
    if (buffer==null) {
      return new OTaskCompletion()
        .setErrorMsg("No task completion");
    }
    try {
      return mapper.readValue(buffer.getBytes(), OTaskCompletion.class);
    } catch (IOException e) {
      if (e instanceof DatabindException databindException) {
        return new OTaskCompletion()
          .setErrorMsg(
          "Can not parse the task completion message. [%s]"
            .formatted(databindException.getMessage()));
      } else {
        return new OTaskCompletion()
          .setErrorMsg(
            "Can not parse the task completion message.");
      }
    }
  }
}
