package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.EntityConverters;
import org.hpcclab.msc.object.entity.BaseUuidEntity;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
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
public class OaasObject extends BaseUuidEntity {


  @Convert(converter = EntityConverters.OriginConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasObjectOrigin origin;

  Long originHash;

  @Enumerated
  ObjectType type;

  @Enumerated
  AccessModifier access;

  @SuppressWarnings("JpaAttributeTypeInspection")
  @Convert(converter = EntityConverters.MapConverter.class)
  @Column(columnDefinition = "jsonb")
  Map<String, String> labels;

  @ManyToMany
  Set<OaasFunction> functions = Set.of();

  @Convert(converter = EntityConverters.StateConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasObjectState state;

  @ElementCollection
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
      labels==null ? null:Map.copyOf(labels),
      functions==null ? null:Set.copyOf(functions),
      state,
      members==null ? null:Set.copyOf(members)
    );
    o.setId(getId());
    return o;
  }

  public static OaasObject createFromClasses(Set<OaasClass> classList) {
    // TODO
    return new OaasObject();
  }

  public void updateHash() {
    this.originHash = origin.hash();
  }

  @Override
  public OaasObject setId(UUID id) {
    return (OaasObject) super.setId(id);
  }
}
