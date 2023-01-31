package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskIdentity {
  String mId;
  String oId;
  String vId;

  public TaskIdentity() {
  }

  @JsonCreator
  public TaskIdentity(String id) {
    var split = id.split(DISCRIMINATOR, -1);
    if (split.length!=3)
      throw new IllegalArgumentException("Wrong ID format");
    mId = Objects.equals(split[0], "") ? null:split[0];
    oId = Objects.equals(split[1], "") ? null:split[1];
    vId = Objects.equals(split[2], "") ? null:split[2];
  }

  public TaskIdentity(String mId, String oId, String vId) {
    this.mId = mId;
    this.oId = oId;
    this.vId = vId;
  }

  public TaskIdentity(TaskDetail task) {
    mId = task.getMain()!=null ? task.getMain().getId():null;
    oId = task.getOutput()!=null ? task.getOutput().getId():null;
    vId = task.getVId();
  }

  public String mId() {
    return mId;
  }

  public String oId() {
    return oId;
  }

  public String vId() {
    return vId;
  }


  public static final String DISCRIMINATOR = ":";

  public String encode() {
    var sb = new StringBuilder();
    if (mId!=null)
      sb.append(mId);
    sb.append(DISCRIMINATOR);
    if (oId!=null)
      sb.append(oId);
    sb.append(DISCRIMINATOR);
    if (vId!=null)
      sb.append(vId);
    return sb.toString();
  }

  public static TaskIdentity decode(String id) {
    return new TaskIdentity(id);
  }

  public static String createEncodedIdFromTask(TaskDetail task) {
    var sb = new StringBuilder();
    if (task.getMain()!=null)
      sb.append(task.getMain().getId());
    sb.append(DISCRIMINATOR);
    if (task.getOutput()!=null)
      sb.append(task.getOutput().getId());
    sb.append(DISCRIMINATOR);
    if (task.getVId()!=null)
      sb.append(task.getVId());
    return sb.toString();
  }

  @Override
  @JsonValue
  public String toString() {
    return encode();
  }
}
