package org.hpcclab.msc.provisioner;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;
import org.hpcclab.msc.object.entity.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class JobProvisioner {

  private static final Logger LOGGER = LoggerFactory.getLogger( JobProvisioner.class );
  @Inject
  KubernetesClient kubernetesClient;

  @Incoming("tasks")
  public void provision(Task task) {
//    LOGGER.info("task: {}", t.encodePrettily());
//    Task task = t.mapTo(Task.class);
//    LOGGER.info("task: {}", task.getFunctionName());
    var envList = createEnv(task)
      .entrySet()
      .stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
      .collect(Collectors.toList());

    var function = task.getFunction();

    var job = new JobBuilder()
      .withNewMetadata()
      .withName(function.getName().replaceAll("/", "-") + "-" + RandomStringUtils.randomNumeric(6))
      .addToLabels("oaas.function", function.getName())
      .addToLabels("oaas.object.main", task.getMain().getId().toString())
      .addToLabels("oaas.object.output", task.getOutput().getId().toString())
      .addToAnnotations("oaas.task", Json.encode(task))
      .endMetadata()
      .withNewSpec()
      .withBackoffLimit(0)
      .withTtlSecondsAfterFinished(120)
      .withNewTemplate()
      .withNewSpec()
      .addNewContainer()
      .withEnv(envList)
      .withName("worker")
      .withImage(function.getTask().getImage())
      .withCommand(function.getTask().getCommands())
      .withArgs(function.getTask().getContainerArgs())
      .endContainer()
      .withRestartPolicy("Never")
      .endSpec()
      .endTemplate()
      .endSpec()
      .build();
    job = kubernetesClient.batch().v1().jobs().create(job);
    LOGGER.info("job {}", Json.encodePrettily(job));

  }

  private Map<String,String> createEnv(Task task) {
    var function = task.getFunction();
    var mainObj = task.getMain();
    var outputObj = task.getOutput();
    var inputs = task.getAdditionalInputs();
    var requestFile = task.getRequestFile();
    var env  = new HashMap<String, String>();
//    if (function.getTask().isArgsToEnv() && outputObj.getOrigin().getArgs()!=null) {
//      env.putAll(outputObj.getOrigin().getArgs());
//    }
//    env.put("TASK_ID", task.getId());
//    putEnv(env, mainObj, "MAIN");
//    for (int i = 0; i < inputs.size(); i++) {
//      OaasObject inputObj = inputs.get(i);
//      var prefix = "INPUT_" + i;
//      putEnv(env, inputObj, prefix);
//    }
//    env.put("OUTPUT_RESOURCE_BASE_URL", outputObj.getState().getBaseUrl());
//    env.put("REQUEST_FILE", requestFile);
    //TODO
    return env;
  }

  private void putEnv(Map<String, String> env, OaasObject obj, String prefix) {
    env.put(prefix + "_ID", obj.getId().toString());
    env.put(prefix + "_RESOURCE_BASE_URL", obj.getState().getBaseUrl());
    env.put(prefix + "_RESOURCE_TYPE", obj.getState().getType().toString());
    if (obj.getState().getType() == OaasObjectState.StateType.FILE)
      env.put(prefix + "_RESOURCE_FILE", obj.getState().getFile());
    if (obj.getState().getType() == OaasObjectState.StateType.FILES)
      env.put(prefix + "_RESOURCE_FILES", String.join(", ",obj.getState().getFiles()));
  }

}
