package org.hpcclab.oaas.entity.task;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.BaseEntity;
import org.hpcclab.oaas.entity.object.OaasObject;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
@Accessors(chain = true)
public class TaskDependent extends BaseEntity {
  @ManyToOne
  TaskFlow required;
  @ManyToOne
  TaskFlow to;

}
