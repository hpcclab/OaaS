package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.infinispan.protostream.annotations.Proto;

import java.util.Map;

/**
 * @author Pawissanutt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Proto
public class OMeta implements IOObject.IOMeta, Copyable<OMeta> {
  String id;
  long revision;
  String cls;
  Map<String, String> verIds;
  Map<String, String> refs;
  long lastOffset;

  @Override
  public OMeta copy() {
    return new OMeta(
      id,
      revision,
      cls,
      verIds,
      refs,
      lastOffset
    );
  }
}
