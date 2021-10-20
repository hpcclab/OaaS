package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hpcclab.msc.object.EntityConverters;
import org.hpcclab.msc.object.entity.BaseUuidEntity;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import javax.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@NamedEntityGraph(
  name = "oaas.object.tree",
  attributeNodes = {
    @NamedAttributeNode(value = "cls", subgraph = "oaas.classes.tree"),
    @NamedAttributeNode(value = "functions", subgraph = "oaas.functionBinding.tree"),
  },
  subgraphs = {
    @NamedSubgraph(name = "oaas.classes.tree",
      attributeNodes = @NamedAttributeNode(value = "functions", subgraph = "oaas.functionBinding.tree")),
    @NamedSubgraph(name = "oaas.functionBinding.tree",
      attributeNodes = @NamedAttributeNode(value = "function", subgraph = "oaas.function.tree")) ,
    @NamedSubgraph(name = "oaas.function.tree",
      attributeNodes = @NamedAttributeNode(value = "outputCls"))
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

  @ManyToOne(fetch = FetchType.LAZY)
  OaasClass cls;

  @SuppressWarnings("JpaAttributeTypeInspection")
  @Convert(converter = EntityConverters.MapConverter.class)
  @Column(columnDefinition = "jsonb")
  Map<String, String> labels;

  @ElementCollection
  List<OaasFunctionBinding> functions = List.of();

  @Convert(converter = EntityConverters.StateConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasObjectState state;

  @ElementCollection
  @LazyCollection(LazyCollectionOption.FALSE)
  List<OaasCompoundMember> members;

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
      functions==null ? null:List.copyOf(functions),
      state,
      members==null ? null:List.copyOf(members)
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
}
