package org.hpcclab.oaas.model.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author Pawissanutt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OMeta implements IOObject.IOMeta{
  String id;
  long revision;
  String cls;
  Map<String, String> verIds;
  Map<String, String> refs;
  long lastOffset;

}
