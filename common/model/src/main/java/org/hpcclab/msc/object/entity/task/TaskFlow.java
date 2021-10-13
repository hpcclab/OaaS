package org.hpcclab.msc.object.entity.task;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@TypeDef(name = "json", typeClass = JsonType.class)
public class TaskFlow {

  @Id
  String id;
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  Task task;
  @ElementCollection
  Set<String> prerequisiteTasks;
  Boolean submitted = false;
}
