package org.hpcclab.msc;

import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.msc.object.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class WorkerProvisioner {

  private static final Logger LOGGER = LoggerFactory.getLogger( WorkerProvisioner.class );
  @Inject
  KubernetesClient kubernetesClient;

  @Incoming("tasks")
  public void provision(JsonObject t) {
    LOGGER.info("task: {}", t.encodePrettily());
    Task task = t.mapTo(Task.class);
//    LOGGER.info("task: {}", task.getFunctionName());
//    var job = new JobBuilder()
//      .withNewMetadata()
//      .withName(task.getFunctionName().replaceAll("/", "-") + "-" + RandomStringUtils.randomAlphanumeric(4))
//      .addToLabels("oaas.function", task.getFunctionName())
//      .endMetadata()
//      .withNewSpec()
//      .withTtlSecondsAfterFinished(120)
//      .withNewTemplate()
//      .withNewSpec()
//      .addNewContainer()
//      .withName("ffmpeg")
//      .withImage(task.getImage())
//      .withCommand(task.getCommands())
//      .withArgs(task.getContainerArgs())
//      .endContainer()
//      .withRestartPolicy("Never")
//      .endSpec()
//      .endTemplate()
//      .endSpec()
//      .build();
//    kubernetesClient.batch().v1().jobs().create(job);
  }

}
