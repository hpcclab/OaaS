package org.hpcclab.msc.object.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
//  @BsonId
  @Id
  String id;
  String mainObj;
  String outputObj;
  String functionName;
  Status status;
  String startTime;
  String completionTime;
  String requestFile;
  String resourceUrl;
  String debugCondition;
  String debugLog;

  public enum Status {
    SUCCEEDED, FAILED
  }
}
