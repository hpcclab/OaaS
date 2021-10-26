package org.hpcclab.oaas.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;
import org.hpcclab.oaas.EntityConverters;
import org.hpcclab.oaas.entity.BaseUuidEntity;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.state.OaasObjectState;

import javax.persistence.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
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
//        ,
//        subgraph = "oaas.functionBinding.tree"
      )),
    @NamedSubgraph(name = "oaas.functionBinding.deep",
      attributeNodes = @NamedAttributeNode(value = "function"
//        , subgraph = "oaas.function.tree"
      )),

//    ,
//    @NamedSubgraph(name = "oaas.function.tree",
//      attributeNodes = @NamedAttributeNode(value = "outputCls"))
  })
public class OaasObject extends BaseUuidEntity {


  @Convert(converter = EntityConverters.OriginConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasObjectOrigin origin;

  Long originHash;

  @Enumerated
  ObjectType type;

  @Enumerated
  AccessModifier access = AccessModifier.PUBLIC;

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

  public enum ObjectType {
    RESOURCE,
    COMPOUND
  }

  public enum AccessModifier {
    PUBLIC,
    INTERNAL,
    FINAL
  }

  public void format() {
    if (type==ObjectType.COMPOUND) {
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
      type,
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
      .setType(cls.getObjectType())
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
}
