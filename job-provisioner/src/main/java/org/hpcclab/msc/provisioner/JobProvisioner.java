package org.hpcclab.msc.provisioner;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.entity.task.OaasTask;
import org.hpcclab.oaas.model.OaasObjectDto;
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
  public void provision(OaasTask task) {
    var envList = createEnv(task)
      .entrySet()
      .stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
      .toList();

    var function = task.getFunction();

    var job = new JobBuilder()
      .withNewMetadata()
      .withName(function.getName().replace("/", "-") + "-" + RandomStringUtils.randomNumeric(6))
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

  private Map<String,String> createEnv(OaasTask task) {
    var function = task.getFunction();
    var mainObj = task.getMain();
    var outputObj = task.getOutput();
    var inputs = task.getAdditionalInputs();
    var requestFile = task.getRequestFile();
    var env  = new HashMap<String, String>();
    if (function.getTask().isArgsToEnv() && outputObj.getOrigin().getArgs()!=null) {
      env.putAll(outputObj.getOrigin().getArgs());
    }
    env.put("TASK_ID", task.getId());
    putEnv(env, mainObj, "MAIN");
    for (int i = 0; i < inputs.size(); i++) {
      var inputObj = inputs.get(i);
      var prefix = "INPUT_" + i;
      putEnv(env, inputObj, prefix);
    }
    env.put("OUTPUT_RESOURCE_BASE_URL", outputObj.getState().getBaseUrl());
    env.put("REQUEST_FILE", requestFile);
    return env;
  }

  private void putEnv(Map<String, String> env, OaasObjectDto obj, String prefix) {
    env.put(prefix + "_ID", obj.getId().toString());
    env.put(prefix + "_RESOURCE_BASE_URL", obj.getState().getBaseUrl());
    if (obj.getState().getFile() != null) {
      env.put(prefix + "_RESOURCE_FILE", obj.getState().getFile());
    }
    env.put(prefix + "_RESOURCE_TYPE", obj.getState().getType().toString());
  }

}
