package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
  String mainObj;
  String outputObj;
  String functionName;
  String image;
  List<String> commands;
  List<String> containerArgs;
  Map<String, String> env = Map.of();
}
