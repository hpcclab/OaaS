package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import org.hpcclab.oaas.model.invocation.InvocationContext;

import java.util.Objects;

@Data
public class TaskIdentity {
  String mid;
  String oid;
  String iid;

  public TaskIdentity() {
  }

  @JsonCreator
  public TaskIdentity(String id) {
    var split = id.split(DISCRIMINATOR, -1);
    if (split.length!=3)
      throw new IllegalArgumentException("Wrong ID format");
    mid = Objects.equals(split[0], "") ? null:split[0];
    oid = Objects.equals(split[1], "") ? null:split[1];
    iid = Objects.equals(split[2], "") ? null:split[2];
  }

  public TaskIdentity(String mId, String oId, String vId) {
    this.mid = mId;
    this.oid = oId;
    this.iid = vId;
  }

  public TaskIdentity(InvocationContext context) {
    mid = context.getMain().getId();
    oid = context.getOutput() != null? context.getOutput().getId(): null;
    iid = context.getRequest().invId();
  }

  public String mid() {
    return mid;
  }

  public String oid() {
    return oid;
  }

  public String iid() {
    return iid;
  }


  public static final String DISCRIMINATOR = ":";

  @JsonValue
  public String encode() {
    var sb = new StringBuilder();
    if (mid!=null)
      sb.append(mid);
    sb.append(DISCRIMINATOR);
    if (oid!=null)
      sb.append(oid);
    sb.append(DISCRIMINATOR);
    if (iid!=null)
      sb.append(iid);
    return sb.toString();
  }

  public static TaskIdentity decode(String id) {
    return new TaskIdentity(id);
  }

  @Override
  public String toString() {
    return encode();
  }
}
