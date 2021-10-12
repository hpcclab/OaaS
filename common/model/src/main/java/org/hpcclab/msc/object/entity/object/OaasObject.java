package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@TypeDef(name = "json", typeClass = JsonType.class)
public class OaasObject {
  //  @BsonId
//  ObjectId id;
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(
    name = "UUID",
    strategy = "org.hibernate.id.UUIDGenerator"
  )
  UUID id;
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  OaasObjectOrigin origin;
  long originHash;
  @Enumerated
  ObjectType type;
  @Enumerated
  AccessModifier access;

  //  Map<String, String> labels;
  @ManyToMany
  Set<OaasFunction> functions = Set.of();
  @Type(type = "json")
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
    if (origin==null) origin = new OaasObjectOrigin().setRootId(id);
  }

  public OaasObject copy() {
    return new OaasObject(
      id,
      origin==null ? null:origin.copy(),
      originHash,
      type,
      access,
//      labels==null ? null:Map.copyOf(labels),
      functions==null ? null:Set.copyOf(functions),
      state,
      members==null ? null:Set.copyOf(members)
    );
  }

  public static OaasObject createFromClasses(List<OaasClass> classList) {
    // TODO
    return new OaasObject();
  }

  public void updateHash() {
    this.originHash = origin.hash();
  }
}
