package org.hpcclab.oaas.model.object;

import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Map;

/**
 * @author Pawissanutt
 */
public interface IOObject<T> extends Copyable<IOObject<T>>, HasKey<String>, HasRev {
  IOMeta getMeta();
  T getData();
  interface IOMeta{
    String getId();
    long getRevision();
    String getCls();
    Map<String, String> getVerIds();
    Map<String, String> getRefs();
    long getLastOffset();
  }
}
