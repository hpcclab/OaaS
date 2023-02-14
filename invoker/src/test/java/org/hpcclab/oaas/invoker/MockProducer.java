package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.Mock;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.test.MockSyncInvoker;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Optional;


@ApplicationScoped
public class MockProducer {
  @Inject
  ObjectMapper objectMapper;
  @Produces
  @Mock
  MockSyncInvoker invoker() {
    var mockSyncInvoker = new MockSyncInvoker();
    mockSyncInvoker.setMapper(detail -> {
      OaasTask task = (OaasTask) detail.getContent();
      ObjectUpdate mainUpdate = null;
      ObjectUpdate outUpdate = null;
      var n = Optional.ofNullable(task.getMain())
        .map(OaasObject::getData)
        .map(on -> on.get("n").asInt(0))
        .orElse(0);
      var data = objectMapper.createObjectNode()
        .put("n", n + 1);
      if (!task.isImmutable()) {
        mainUpdate = new ObjectUpdate(data);
      }
      if (task.getOutput() != null) {
        outUpdate = new ObjectUpdate(data);
      }

      return new TaskCompletion()
        .setId(TaskIdentity.decode(detail.getId()))
        .setMain(mainUpdate)
        .setOutput(outUpdate)
        .setSuccess(true);
    });
    return mockSyncInvoker;
  }
}
