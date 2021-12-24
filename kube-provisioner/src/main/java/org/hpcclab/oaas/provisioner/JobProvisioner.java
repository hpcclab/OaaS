package org.hpcclab.oaas.provisioner;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.task.OaasTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ApplicationScoped
public class JobProvisioner {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobProvisioner.class);
  @Inject
  KubernetesClient kubernetesClient;

  Random random = new Random();

  //  @Incoming("tasks")
  public void provision(OaasTask task) {
    var envList = createEnv(task)
      .entrySet()
      .stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
      .toList();

    var function = task.getFunction();

    var jobProvisionConfig = task.getFunction().getProvision().getJob();
    if (jobProvisionConfig == null)
      throw new NoStackException("function.provision.job must not be null");

    Map<String, Quantity> requests = new HashMap<>();
    if (jobProvisionConfig.getRequestsCpu() != null) {
      requests.put("cpu", Quantity.parse(jobProvisionConfig.getRequestsCpu()));
    }
    if (jobProvisionConfig.getRequestsMemory() != null) {
      requests.put("memory", Quantity.parse(jobProvisionConfig.getRequestsMemory()));
    }
    Map<String, Quantity> limits = new HashMap<>();
    if (jobProvisionConfig.getLimitsCpu() != null) {
      limits.put("cpu", Quantity.parse(jobProvisionConfig.getLimitsCpu()));
    }
    if (jobProvisionConfig.getLimitsMemory() != null) {
      limits.put("memory", Quantity.parse(jobProvisionConfig.getLimitsMemory()));
    }

    ResourceRequirements resourceRequirements = new ResourceRequirements(limits, requests);

    var job = new JobBuilder()
      .withNewMetadata()
      .withName(function.getName().replace("/", "-") + "-" +
        generateRandomString(8))
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
      .withImage(jobProvisionConfig.getImage())
      .withCommand(jobProvisionConfig.getCommands())
      .withArgs(jobProvisionConfig.getContainerArgs())
      .withResources(resourceRequirements)
      .endContainer()
      .withRestartPolicy("Never")
      .endSpec()
      .endTemplate()
      .endSpec()
      .build();
    job = kubernetesClient.batch().v1().jobs().create(job);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("job {}", Json.encodePrettily(job));
    }
  }

  private Map<String, String> createEnv(OaasTask task) {
    var function = task.getFunction();
    var mainObj = task.getMain();
    var outputObj = task.getOutput();
    var inputs = task.getAdditionalInputs();
    var requestFile = task.getRequestFile();
    var jobProvisionConfig = task.getFunction().getProvision().getJob();
    var env = new HashMap<String, String>();
    if (jobProvisionConfig.isArgsToEnv() && outputObj.getOrigin().getArgs()!=null) {
      env.putAll(outputObj.getOrigin().getArgs());
    }
    env.put("TASK_ID", task.getId());
    putEnv(env, mainObj, "MAIN");
    for (int i = 0; i < inputs.size(); i++) {
      var inputObj = inputs.get(i);
      var prefix = "INPUT_" + i;
      putEnv(env, inputObj, prefix);
    }
    putEnv(env, outputObj, "OUTPUT");
    env.put("REQUEST_FILE", requestFile);
    return env;
  }

  private void putEnv(Map<String, String> env, OaasObject obj, String prefix) {
    env.put(prefix + "_ID", obj.getId().toString());
    env.put(prefix + "_RESOURCE_BASE_URL", obj.getState().getBaseUrl());
    if (obj.getState().getKeys()!=null) {
      for (int i = 0; i < obj.getState().getKeys().size(); i++) {
        env.put(prefix + "_RESOURCE_FILE_" + i, obj.getState().getKeys().get(i));
      }
    }
    env.put(prefix + "_RESOURCE_TYPE", obj.getState().getType().toString());
  }

  String generateRandomString (int length) {
    return String.format("%0" + length + "d", random.nextLong());
  }
}
