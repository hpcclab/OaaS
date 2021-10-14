package org.hpcclab.msc.object.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
//  @BsonId
  @Id
  String id;
  UUID mainObj;
  UUID outputObj;
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
