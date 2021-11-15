package org.hpcclab.oaas.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.UUIDGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
@RegisterForReflection(targets = UUIDGenerator.class)
public class BaseUuidEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(
    name = "UUID",
    strategy = "org.hibernate.id.UUIDGenerator"
  )
  protected UUID id;
}
