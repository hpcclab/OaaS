package org.hpcclab.oaas.model.task;

public abstract class BaseTaskMessage {
  protected String id;

  public String getId() {
    return id;
  }

  public BaseTaskMessage setId(String id) {
    this.id = id;
    return this;
  }
}
