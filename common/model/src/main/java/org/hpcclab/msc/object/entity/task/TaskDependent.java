package org.hpcclab.msc.object.entity.task;


import lombok.Getter;
import lombok.Setter;
import org.hpcclab.msc.object.entity.BaseEntity;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
public class TaskDependent extends BaseEntity {
  @ManyToOne
  OaasObject required;
  @ManyToOne
  OaasObject to;
}
