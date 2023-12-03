package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class InvocationNode implements HasKey<String> {
  @JsonProperty("_key")
  @ProtoField(1)
  String key;
  @ProtoField(2)
  Set<InvocationRef> nextInv;
  @ProtoField(3)
  String fb;
  @ProtoField(4)
  String main;
  @ProtoField(5)
  String cls;
  @ProtoField(6)
  DSMap args;
  @ProtoField(7)
  List<String> inputs;
  @ProtoField(8)
  String outId;
  @ProtoField(9)
  String originator;
  @ProtoField(10)
  Set<String> waitFor;
  @ProtoField(11)
  InvocationStatus status = InvocationStatus.LAZY;
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

  @ProtoFactory
  public InvocationNode(String key, Set<InvocationRef> nextInv, String fb, String main, String cls, DSMap args, List<String> inputs, String outId, String originator, Set<String> waitFor, InvocationStatus status, long queTs, long smtTs, long cptTs) {
    this.key = key;
    this.nextInv = nextInv;
    this.fb = fb;
    this.main = main;
    this.cls = cls;
    this.args = args;
    this.inputs = inputs;
    this.outId = outId;
    this.originator = originator;
    this.waitFor = waitFor;
    this.status = status;
    this.queTs = queTs;
    this.smtTs = smtTs;
    this.cptTs = cptTs;
  }


  public Set<InvocationRef> getNextInv() {
    if (nextInv==null) nextInv = Sets.mutable.empty();
    return nextInv;
  }

  public InvocationRequest.InvocationRequestBuilder toReq() {

    var partKey = main!=null ? main:null;
    return InvocationRequest.builder()
      .invId(key)
      .partKey(partKey)
      .main(main)
      .cls(cls)
      .args(args)
      .fb(fb)
      .inputs(inputs)
      .outId(outId)
      .queTs(System.currentTimeMillis())
      .preloadingNode(true);
  }

  public InvocationNode trigger(String originator, String srcId) {
    waitFor.remove(srcId);
    if (status.isSubmitted() || status.isFailed() || !waitFor.isEmpty())
      return this;
    status = InvocationStatus.DOING;
    this.originator = originator;
    return this;
  }

  public InvocationNode markAsSubmitted(String originator,
                                        boolean queue) {
    if (status.isSubmitted() || status.isFailed())
      return this;
    if (originator==null)
      this.originator = key;
    else
      this.originator = originator;
    status = InvocationStatus.DOING;
    if (queue)
      queTs = System.currentTimeMillis();
    else
      smtTs = System.currentTimeMillis();
    return this;
  }


  public InvocationNode markAsFailed() {
    if (status.isSubmitted() || status.isFailed())
      return this;
    status = InvocationStatus.DEPENDENCY_FAILED;
    return this;
  }

  public InvocationStats extractStats() {
    return new InvocationStats(queTs, smtTs, cptTs);
  }

  public void updateStatus(TaskCompletion completion) {
    if (completion.isSuccess()) {
      status = InvocationStatus.SUCCEEDED;
    } else
      status = InvocationStatus.FAILED;
    if (completion.getCptTs() > 0) {
      cptTs = completion.getCptTs();
    } else {
      cptTs = System.currentTimeMillis();
    }
    if (completion.getSmtTs() > 0) {
      smtTs = completion.getSmtTs();
    }
  }


}
