package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateType;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class OOUpdate {
  ObjectNode data;
  DSMap refs = DSMap.of();
  Set<String> updatedKeys = Set.of();

  public OOUpdate() {
  }

  public OOUpdate(ObjectNode data) {
    this.data = data;
  }

  public OOUpdate(ObjectNode data,
                  DSMap refs,
                  Set<String> updatedKeys) {
    this.data = data;
    this.refs = refs;
    this.updatedKeys = updatedKeys;
  }

  public void filterKeys(OClass cls) {
    if (updatedKeys==null || updatedKeys.isEmpty())
      return;
    if (cls.getStateType()==StateType.COLLECTION)
      return;
    updatedKeys = Sets.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .collect(KeySpecification::getName)
      .intersect(Sets.fixedSize.withAll(updatedKeys));
  }

  public void update(GOObject obj, String newVerId) {
    if (obj==null)
      return;

    if (data!=null)
      obj.setData(new JsonBytes(data));

    if (refs!=null && !refs.isEmpty()) {
      var map = DSMap.copy(obj.getMeta().getRefs());
      map.putAll(refs);
      obj.getMeta().setRefs(map);
    }

    if (updatedKeys!=null && !updatedKeys.isEmpty()) {
      var verIds = DSMap.wrap(Sets.fixedSize.ofAll(updatedKeys)
        .toMap(k -> k, __ -> newVerId)
      );
      var oldVerIds = obj.getMeta().getVerIds();
      if (oldVerIds==null || oldVerIds.isEmpty())
        obj.getMeta().setVerIds(verIds);
      else {
        var tmp = Maps.mutable.ofMap(oldVerIds);
        tmp.putAll(verIds);
        obj.getMeta().setVerIds(DSMap.wrap(tmp));
      }
    }
  }
}
