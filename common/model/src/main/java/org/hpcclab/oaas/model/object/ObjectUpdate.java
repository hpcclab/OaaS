package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateType;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class ObjectUpdate {
  ObjectNode data;
  DSMap refs = DSMap.of();
  Set<String> updatedKeys = Set.of();

  public ObjectUpdate() {
  }

  public ObjectUpdate(ObjectNode data) {
    this.data = data;
  }

  public ObjectUpdate(ObjectNode data,
                      DSMap refs,
                      Set<String> updatedKeys) {
    this.data = data;
    this.refs = refs;
    this.updatedKeys = updatedKeys;
  }

  public void filterKeys(OaasClass cls) {
    if (updatedKeys==null || updatedKeys.isEmpty())
      return;
    if (cls.getStateType()==StateType.COLLECTION)
      return;
    updatedKeys = Sets.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .collect(KeySpecification::getName)
      .intersect(Sets.fixedSize.withAll(updatedKeys));
  }

  public void update(OaasObject obj, String newVerId) {
    if (obj==null)
      return;

    if (data!=null)
      obj.setData(data);

    if (refs!=null && !refs.isEmpty()) {
      var map = DSMap.copy(obj.getRefs());
      map.putAll(refs);
      obj.setRefs(map);
    }

    if (updatedKeys!=null && !updatedKeys.isEmpty()) {
      var verIds = DSMap.wrap(Sets.fixedSize.ofAll(updatedKeys)
        .toMap(k -> k, __ -> newVerId)
      );
      var oldVerIds = obj.getState().getVerIds();
      if (oldVerIds==null || oldVerIds.isEmpty())
        obj.getState().setVerIds(verIds);
      else {
        var tmp = Maps.mutable.ofMap(oldVerIds);
        tmp.putAll(verIds);
        obj.getState().setVerIds(DSMap.wrap(tmp));
      }
    }
  }
}
