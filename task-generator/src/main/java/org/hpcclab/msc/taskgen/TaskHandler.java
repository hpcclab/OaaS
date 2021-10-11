package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.task.TaskCompletion;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.entity.task.Task;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import io.smallrye.reactive.messaging.kafka.Record;
import org.hpcclab.msc.object.service.ObjectService;
import org.hpcclab.msc.taskgen.repository.TaskCompletionRepository;
import org.hpcclab.msc.taskgen.repository.TaskFlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;

@ApplicationScoped
public class TaskHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandler.class);

  @Inject
  TaskFactory taskFactory;
  @Inject
  TaskFlowRepository taskFlowRepo;
  @Inject
  TaskCompletionRepository taskCompletionRepo;

  @Channel("tasks")
  Emitter<Record<String, Task>> tasksEmitter;
  @Inject
  ObjectService objectService;

  public Uni<TaskFlow> handle(ObjectResourceRequest request) {
    if (request.getOwnerObjectId()==null || !ObjectId.isValid(request.getOwnerObjectId())) {
      LOGGER.warn("receive request with invalid ownerObjectId. [{}]", request);
      return Uni.createFrom().failure(new BadRequestException());
    }
    return objectService.get(request.getOwnerObjectId())
      .onItem().ifNull().failWith(NotFoundException::new)
      .flatMap(outputObj -> createFlow(outputObj, request.getRequestFile()));
  }

  private Uni<TaskFlow> createFlow(OaasObject outputObj, String requestFile) {
    if (outputObj.getOrigin().getParentId()==null) {
      return Uni.createFrom().nullItem();
    }
    return taskFlowRepo.find(outputObj, requestFile)
      .onItem().ifNull()
      .switchTo(() -> objectService.loadExecutionContext(outputObj.getId().toString())
        .flatMap(context -> recursiveCreateFlow(context, outputObj, requestFile))
      );
  }

  private Uni<TaskFlow> recursiveCreateFlow(FunctionExecContext context,
                                            OaasObject outputObj,
                                            String requestFile) {
    return taskFlowRepo
      .persist(taskFactory.genTaskSequence(outputObj, requestFile, context))
      .call(flow -> checkSubmittable(flow)
        .flatMap(submitted -> {
          if (!submitted) {
            var l = new ArrayList<OaasObject>();
            l.add(context.getMain());
            l.addAll(context.getAdditionalInputs());
            return Multi.createFrom().iterable(l)
              .onItem().transformToUniAndConcatenate(o -> createFlow(o, requestFile))
              .collect().last();
          }
          return Uni.createFrom().nullItem();
        })
      );
  }

  public Uni<Boolean> checkSubmittable(TaskFlow taskFlow) {
    if (taskFlow.getPrerequisiteTasks().size()==0) {
      return submitTask(taskFlow)
        .map(f -> true);
    }
//    LOGGER.debug("flow {}, require {}", taskFlow.getId(), taskFlow.getPrerequisiteTasks());
    return taskCompletionRepo.find("_id in ?1", taskFlow.getPrerequisiteTasks())
      .list()
      .flatMap(taskCompletions -> {
        LOGGER.debug("checkSubmittable {} count: {}", taskFlow.getId(), taskCompletions.size());
        if (taskFlow.getPrerequisiteTasks().size() <= taskCompletions.size()) {
          boolean succeeded = true;
          for (TaskCompletion taskCompletion : taskCompletions) {
            succeeded &= taskCompletion.getStatus()==TaskCompletion.Status.SUCCEEDED;
          }
          if (succeeded) {
            return submitTask(taskFlow)
              .map(f -> true);
          }
        }
        return Uni.createFrom().item(false);
      });
  }

  private Uni<TaskFlow> submitTask(TaskFlow flow) {
    var record = Record.of(flow.getId(), flow.getTask());
    return Uni.createFrom()
      .completionStage(tasksEmitter.send(record))
      .flatMap(v -> {
        flow.setSubmitted(true);
        return taskFlowRepo.update(flow);
      });
  }
}
