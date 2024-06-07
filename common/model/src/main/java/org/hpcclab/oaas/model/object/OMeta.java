package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Proto
public class OMeta implements IOObject.IOMeta{
  String id;
  long revision;
  String cls;
  Map<String, String> verIds;
  Map<String, String> refs;
  long lastOffset;

}
