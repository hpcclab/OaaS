package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.task.TaskCompletion;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.model.Task;
import org.hpcclab.msc.object.service.FunctionService;
import org.hpcclab.msc.object.service.ObjectService;
import org.hpcclab.msc.taskgen.repository.TaskCompletionRepository;
import org.hpcclab.msc.taskgen.repository.TaskFlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.ArrayList;

@ApplicationScoped
public class TaskHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandler.class);

  @Inject
  TaskFactory taskFactory;
  @Inject
  TaskFlowRepository taskSequenceRepo;
  @Inject
  TaskCompletionRepository taskCompletionRepo;

  @Channel("tasks")
  Emitter<Task> tasksEmitter;
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

  private Uni<TaskFlow> createFlow(MscObject outputObj, String requestFile) {
    if (outputObj.getOrigin().getParentId()==null) {
      return Uni.createFrom().nullItem();
    }
    return taskSequenceRepo.find(outputObj, requestFile)
      .onItem().ifNull()
      .switchTo(() -> objectService.loadExecutionContext(outputObj.getId().toString())
        .flatMap(context -> taskSequenceRepo
          .persist(taskFactory.genTaskSequence(outputObj, requestFile, context))
          .flatMap(this::checkSubmittable)
          .flatMap(submitted -> {
            if (!submitted) {
              var l = new ArrayList<MscObject>();
              l.add(context.getTarget());
              l.addAll(context.getAdditionalInputs());
              return Multi.createFrom().iterable(l)
                .onItem().transformToUniAndConcatenate(o -> createFlow(o, requestFile))
                .collect().last();
            }
            return Uni.createFrom().nullItem();
          })
        )
      );
  }

  public Uni<Boolean> checkSubmittable(TaskFlow taskFlow) {
    if (taskFlow.getPrerequisiteTasks().size()==0) {
      return submitTask(taskFlow)
        .map(f -> true);
    }
    return taskCompletionRepo.find("id in ?1", taskFlow.getPrerequisiteTasks())
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
    return Uni.createFrom().completionStage(tasksEmitter.send(flow.getTask()))
      .flatMap(v -> {
        flow.setSubmitted(true);
        return taskSequenceRepo.update(flow);
      });
  }
}
