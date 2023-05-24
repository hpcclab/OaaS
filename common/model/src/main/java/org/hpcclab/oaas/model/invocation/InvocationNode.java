package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.proto.KvPair;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class InvocationNode implements HasKey {
  @JsonProperty("_key")
  @ProtoField(1)
  String key;
  @ProtoField(2)
  Set<String> nextInv;
  @ProtoField(3)
  String fb;
  @ProtoField(4)
  String target;
  @ProtoField(5)
  String targetCls;
  @ProtoField(6)
  Set<KvPair> args;
  @ProtoField(7)
  List<String> inputs;
  @ProtoField(8)
  String outId;
  @ProtoField(9)
  String originator;
  @ProtoField(10)
  Set<String> waitFor;
  @ProtoField(11)
  TaskStatus status;
  @ProtoField(value = 12, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long queTs;
  @ProtoField(value = 13, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long smtTs;
  @ProtoField(value = 14, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long cptTs;

  public InvocationNode() {
  }

  public InvocationNode(String key) {
    this.key = key;
    this.nextInv = new HashSet<>();
  }


  @ProtoFactory
  public InvocationNode(String key, Set<String> nextInv, String fb, String target, String targetCls, Set<KvPair> args, List<String> inputs, String outId, String originator) {
    this.key = key;
    this.nextInv = nextInv;
    this.fb = fb;
    this.target = target;
    this.targetCls = targetCls;
    this.args = args;
    this.inputs = inputs;
    this.outId = outId;
    this.originator = originator;
  }


  public Set<String> getNextInv() {
    if (nextInv==null) nextInv = Sets.mutable.empty();
    return nextInv;
  }

  public InvocationRequest toReq() {
    return InvocationRequest.builder()
      .invId(key)
      .target(target)
      .targetCls(targetCls)
      .args(KvPair.toMap(args))
      .fb(fb)
      .inputs(inputs)
      .outId(outId)
      .build();
  }

  public InvocationNode trigger(String originator, String srcId) {
    waitFor.remove(srcId);
    if (status.isSubmitted() || status.isFailed() || !waitFor.isEmpty())
      return this;
    status = TaskStatus.DOING;
    this.originator = originator;
    return this;
  }

  public InvocationNode markAsSubmitted(String originator,
                                    boolean queue) {
    if (status.isSubmitted() || status.isFailed())
      return this;
    if (originator == null)
      this.originator = key;
    else
      this.originator = originator;
    status = TaskStatus.DOING;
    if (queue)
      queTs = System.currentTimeMillis();
    else
      smtTs = System.currentTimeMillis();

    return this;
  }


  public InvocationNode markAsFailed() {
    if (status.isSubmitted() || status.isFailed())
      return this;
    status = TaskStatus.DEPENDENCY_FAILED;
    return this;
  }

  public InvocationStats extractStats() {
    return new InvocationStats(queTs, smtTs, cptTs);
  }
}
