package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invoker.service.DataAllocationService;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.object.OOUpdate;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.test.MockOffLoader;

import java.util.List;
import java.util.Optional;


@ApplicationScoped
public class MockProducer {
  @Inject
  ObjectMapper objectMapper;

  @Produces
  @Mock
  MockOffLoader invoker() {
    var mockSyncInvoker = new MockOffLoader();
    mockSyncInvoker.setMapper(detail -> {
      OTask task = (OTask) detail.getContent();
      OOUpdate mainUpdate = null;
      OOUpdate outUpdate = null;
      var n = Optional.ofNullable(task.getMain())
        .map(OObject::getData)
        .map(on -> on.get("n").asInt())
        .orElse(0);
      if (task.getInputs()!=null && !task.getInputs().isEmpty()) {
        for (OObject input : task.getInputs()) {
          var ni = Optional.ofNullable(input)
            .map(OObject::getData)
            .map(on -> on.get("n").asInt())
            .orElse(0);
          n += ni;
        }
      }
      var add = Integer.parseInt(task.getArgs().getOrDefault("ADD", "1"));

      var data = objectMapper.createObjectNode()
        .put("n", n + add);
      if (!task.isImmutable()) {
        mainUpdate = new OOUpdate(data);
      }
      if (task.getOutput()!=null) {
        outUpdate = new OOUpdate(data);
      }

      return new TaskCompletion()
        .setId(detail.getId())
        .setMain(mainUpdate)
        .setOutput(outUpdate)
        .setSuccess(true);
    });
    return mockSyncInvoker;
  }

  @Mock
  @Produces
  DataAllocationService mockDataAllocation() {
    return new DataAllocationService() {
      @Override
      public Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests) {
        var resList = Lists.fixedSize.ofAll(requests)
          .collect(req -> {
            var m = Lists.fixedSize.ofAll(req.getKeys())
              .toMap(KeySpecification::getName, KeySpecification::getName);
            return new DataAllocateResponse(req.getOid(), m);
          });
        return Uni.createFrom().item(resList);
      }
    };
  }
}
