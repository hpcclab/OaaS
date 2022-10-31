package org.hpcclab.oaas.invocation;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDecoder {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskDecoder.class);

  public static TaskCompletion tryDecode(String id, Buffer buffer) {
    var ts = System.currentTimeMillis();
    if (buffer==null) {
      return new TaskCompletion(id, false,
        "Can not parse the task completion message because buffer is null",
        null, null, -1, ts);
    }
    try {
      var completion = Json.decodeValue(buffer, TaskCompletion.class);
      if (completion!=null) {
        return completion
          .setCmpTs(ts);
      }

    } catch (DecodeException decodeException) {
      LOGGER.warn("Decode failed on id {} : {}", id, decodeException.getMessage());
      return new TaskCompletion(
        id,
        true,
        decodeException.getMessage(),
//        null,
        null,
        null,
        -1,
        ts);
    }

    return new TaskCompletion(id, false,
      "Can not parse the task completion message",
      null, null,
      -1, ts);
  }
}
