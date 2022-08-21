package org.hpcclab.oaas.model.object;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
public class StreamInfo implements Copyable<StreamInfo> {
  @ProtoField(number = 1, defaultValue = "0")
  int count = 0;

  public StreamInfo() {
  }

  @ProtoFactory
  public StreamInfo(int count) {
    this.count = count;
  }

  public StreamInfo copy() {
    return new StreamInfo(
      count
    );
  }
}
