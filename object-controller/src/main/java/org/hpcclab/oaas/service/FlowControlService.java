package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.task.OaasTask;
import org.hpcclab.oaas.entity.task.TaskFlow;
import org.hpcclab.oaas.entity.task.TaskDependent;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.FunctionExecContext;
import org.hpcclab.oaas.repository.TaskDependentRepository;
import org.hpcclab.oaas.repository.TaskFlowRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class FlowControlService {

  @Inject
  TaskFlowRepository flowRepo;
  @Inject
  TaskDependentRepository depRepo;
  @Inject
  OaasMapper mapper;


  public Uni<TaskFlow> createFlow(FunctionExecContext ctx,
                                  OaasObject output,
                                  String requestFile) {
    return flowRepo.getSession().flatMap(session -> {
      OaasTask task = new OaasTask()
        .setId(OaasTask.createId(output, requestFile))
        .setMain(mapper.toObject(ctx.getMain()))
        .setOutput(mapper.toObject(output))
        .setAdditionalInputs(mapper.toObject(ctx.getAdditionalInputs()))
        .setFunction(mapper.toFunc(ctx.getFunction()))
        .setRequestFile(requestFile);
      TaskFlow taskFlow = new TaskFlow()
        .setId(task.getId())
        .setTask(task)
        .setSubmitted(false);
      List<TaskDependent> dependents = new ArrayList<>();
      if (ctx.getMain().getOrigin().getParentId()==null) {
        var dep = new TaskDependent()
          .setTo(taskFlow)
//          .setRequired(ctx.getMain())
          ;
        dependents.add(dep);
      }
      for (OaasObject additionalInput : ctx.getAdditionalInputs()) {
        if (additionalInput.getOrigin().getParentId()==null) {
          var adep = new TaskDependent()
            .setTo(taskFlow)
//            .setRequired(additionalInput)
            ;
          dependents.add(adep);
        }
      }
      return flowRepo.persist(taskFlow)
        .call(() -> depRepo.persist(dependents));
    });
  }
}
