package org.hpcclab.msc.provisioner;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.msc.object.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
    var envList = task
      .getEnv()
      .entrySet()
      .stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
      .collect(Collectors.toList());

    var job = new JobBuilder()
      .withNewMetadata()
      .withName(task.getFunctionName().replaceAll("/", "-") + "-" + RandomStringUtils.randomNumeric(6))
      .addToLabels("oaas.function", task.getFunctionName())
      .addToLabels("oaas.object.main", task.getMainObj())
      .addToLabels("oaas.object.output", task.getOutputObj())
      .addToAnnotations("oaas.task", Json.encode(task))
      .endMetadata()
      .withNewSpec()
      .withBackoffLimit(0)
      .withTtlSecondsAfterFinished(120)
      .withNewTemplate()
      .withNewSpec()
      .addNewContainer()
      .withEnv(envList)
      .withName("ffmpeg")
      .withImage(task.getImage())
      .withCommand(task.getCommands())
      .withArgs(task.getContainerArgs())
      .endContainer()
      .withRestartPolicy("Never")
      .endSpec()
      .endTemplate()
      .endSpec()
      .build();
    job = kubernetesClient.batch().v1().jobs().create(job);
    LOGGER.info("job {}", Json.encodePrettily(job));

  }

}
