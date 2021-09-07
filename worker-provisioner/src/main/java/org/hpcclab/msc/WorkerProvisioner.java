package org.hpcclab.msc;

import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.hpcclab.msc.object.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class WorkerProvisioner {

  @Inject
  KubernetesClient kubernetesClient;

  public void provision(Task task) {
    var job = new JobBuilder()
      .withNewMetadata()
      .withName(task.getFunctionName().replaceAll("/", "-") + "-" + RandomStringUtils.randomAlphanumeric(4))
      .addToLabels("oaas.function", task.getFunctionName())
      .endMetadata()
      .withNewSpec()
      .withTtlSecondsAfterFinished(120)
      .withNewTemplate()
      .withNewSpec()
      .addNewContainer()
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
    kubernetesClient.batch().v1().jobs().create(job);
  }

}
