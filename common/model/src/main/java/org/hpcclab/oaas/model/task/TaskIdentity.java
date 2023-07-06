package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskIdentity {
  String mid;
  String oid;
  String vid;

  public TaskIdentity() {
  }

  @JsonCreator
  public TaskIdentity(String id) {
    var split = id.split(DISCRIMINATOR, -1);
    if (split.length!=3)
      throw new IllegalArgumentException("Wrong ID format");
    mid = Objects.equals(split[0], "") ? null:split[0];
    oid = Objects.equals(split[1], "") ? null:split[1];
    vid = Objects.equals(split[2], "") ? null:split[2];
  }

  public TaskIdentity(String mId, String oId, String vId) {
    this.mid = mId;
    this.oid = oId;
    this.vid = vId;
  }

  public TaskIdentity(TaskDetail task) {
    mid = task.getMain()!=null ? task.getMain().getId():null;
    oid = task.getOutput()!=null ? task.getOutput().getId():null;
    vid = task.getVId();
  }

  public String mid() {
    return mid;
  }

  public String oid() {
    return oid;
  }

  public String vid() {
    return vid;
  }


  public static final String DISCRIMINATOR = ":";

  public String encode() {
    var sb = new StringBuilder();
    if (mid!=null)
      sb.append(mid);
    sb.append(DISCRIMINATOR);
    if (oid!=null)
      sb.append(oid);
    sb.append(DISCRIMINATOR);
    if (vid!=null)
      sb.append(vid);
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
