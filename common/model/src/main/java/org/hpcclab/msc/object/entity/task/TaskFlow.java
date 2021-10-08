package org.hpcclab.msc.object.entity.task;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskFlow {
  @BsonId
  String id;
  Task task;
  List<String> prerequisiteTasks;
  boolean submitted = false;
}
