package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.Base64;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataAccessContext {

  public static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  public static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  String id;
  String cls;

  String sig;


  public String encode() {
    String sb = id + ':' + cls;
    return ENCODER.encodeToString(sb.getBytes());
  }

  public static DataAccessContext generate(OaasObject obj, OaasClass cls) {
    var dac = new DataAccessContext();
    dac.id = obj.getId();
    dac.cls = cls.getName();
    return dac;
  }

  public static DataAccessContext generate(OaasObject obj) {
    var dac = new DataAccessContext();
    dac.id = obj.getId();
    dac.cls = obj.getCls();
    return dac;
  }

  public static DataAccessContext parse(String b64) {
    var bytes = DECODER.decode(b64);
    var text = new String(bytes);
    var dac = new DataAccessContext();
    var splitText = text.split(":");
    dac.id = splitText[0];
    dac.cls = splitText[1];
    return dac;
  }
}
