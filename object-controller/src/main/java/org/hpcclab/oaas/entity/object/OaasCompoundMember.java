package org.hpcclab.oaas.entity.object;

import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hpcclab.oaas.entity.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
public class OaasCompoundMember extends BaseEntity {

  String name;

  @NotNull
  @JoinColumn
  @ManyToOne(fetch = FetchType.LAZY)
  @ToString.Exclude
  OaasObject object;

  @Override
  public boolean equals(Object o) {
    if (this==o) return true;
    if (o==null || Hibernate.getClass(this)!=Hibernate.getClass(o)) return false;
    OaasCompoundMember that = (OaasCompoundMember) o;
    return id!=null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
