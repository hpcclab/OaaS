package org.hpcclab.oaas.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hpcclab.oaas.entity.EntityConverters;
import org.hpcclab.oaas.entity.BaseUuidEntity;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.object.ObjectAccessModifier;
import org.hpcclab.oaas.model.state.OaasObjectState;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@NamedEntityGraph(
  name = "oaas.object.find",
  attributeNodes = {
    @NamedAttributeNode(
      value = "members"
    ),
    @NamedAttributeNode(
      value = "functions"
    )
  })
@NamedEntityGraph(
  name = "oaas.object.deep",
  attributeNodes = {
    @NamedAttributeNode(
      value = "cls",
      subgraph = "oaas.classes.deep"
    ),
    @NamedAttributeNode(
      value = "members"
    ),
    @NamedAttributeNode(
      value = "functions",
      subgraph = "oaas.functionBinding.deep"
    )
  },
  subgraphs = {
    @NamedSubgraph(name = "oaas.classes.deep",
      attributeNodes = @NamedAttributeNode(value = "functions"
      )),
    @NamedSubgraph(name = "oaas.functionBinding.deep",
      attributeNodes = @NamedAttributeNode(value = "function"
      )),
  })
public class OaasObject extends BaseUuidEntity {


  @Convert(converter = EntityConverters.OriginConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasObjectOrigin origin;

  Long originHash;

  @Enumerated
  ObjectAccessModifier access = ObjectAccessModifier.PUBLIC;

  @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.DETACH)
  @ToString.Exclude
  OaasClass cls;

  @SuppressWarnings("JpaAttributeTypeInspection")
  @Convert(converter = EntityConverters.MapConverter.class)
  @Column(columnDefinition = "jsonb")
  Map<String, String> labels;

  @ElementCollection
  Set<OaasFunctionBinding> functions = Set.of();

  @Convert(converter = EntityConverters.StateConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasObjectState state;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "owner_id")
  @ToString.Exclude
  Set<OaasCompoundMember> members;

  public void validate() {
    if (getCls().getObjectType() ==OaasObjectType.COMPOUND) {
      state = null;
    } else {
      members = null;
    }
    if (origin==null) origin = new OaasObjectOrigin().setRootId(getId());
  }

  public OaasObject copy() {
    var o = new OaasObject(
      origin==null ? null:origin.copy(),
      originHash,
      access,
      cls,
      labels==null ? null:Map.copyOf(labels),
      functions==null ? null:Set.copyOf(functions),
      state,
      members==null ? null:Set.copyOf(members)
    );
    o.setId(getId());
    return o;
  }

  public static OaasObject createFromClasses(OaasClass cls) {
    return new OaasObject()
      .setCls(cls)
      .setState(new OaasObjectState().setType(cls.getStateType()));
  }

  public void updateHash() {
    this.originHash = origin.hash();
  }

  @Override
  public OaasObject setId(UUID id) {
    return (OaasObject) super.setId(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this==o) return true;
    if (o==null || Hibernate.getClass(this)!=Hibernate.getClass(o)) return false;
    OaasObject that = (OaasObject) o;
    return id!=null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  public Optional<OaasFunctionBinding> findFunction(String name) {
    return Stream.concat(functions.stream(), cls.getFunctions().stream())
      .filter(fb -> fb.getFunction().getName().equals(name))
      .findFirst();
  }

  public Optional<OaasObject> findMember(String name) {
    return members.stream()
      .filter(mem -> mem.getName().equals(name))
      .map(OaasCompoundMember::getObject)
      .findFirst();
  }
}
