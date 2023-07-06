package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
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
  AccessLevel level;
  String vid;
  String sig;

  String b64;

  public DataAccessContext() {
  }

  public String encode() {
    if (b64 == null)
      b64 = genB64();
    return b64;
  }

  public String forceEncode() {
    b64 = genB64();
    return b64;
  }

  private String genB64() {
    StringBuilder sb = new StringBuilder();
    if (id != null)
      sb.append(id);
    sb.append(':');
    if (cls != null)
      sb.append(cls);
    sb.append(':');
    if (level != null)
      sb.append(level.getLevel());
    sb.append(':');
    if (vid != null)
      sb.append(vid);
    sb.append(':');
    if (sig != null)
      sb.append(sig);
    return ENCODER.encodeToString(sb.toString().getBytes());
  }


  public static DataAccessContext generate(OaasObject obj) {
    return generate(obj, AccessLevel.UNIDENTIFIED);
  }



  public static DataAccessContext generate(OaasObject obj,
                                           AccessLevel level) {
    var dac = new DataAccessContext();
    dac.id = obj.getId();
    dac.cls = obj.getCls();
    dac.level = level;
    return dac;
  }

  public static DataAccessContext generate(OaasObject obj,
                                           AccessLevel level,
                                           String vid) {
    var dac = new DataAccessContext();
    dac.id = obj.getId();
    dac.vid = vid;
    dac.cls = obj.getCls();
    dac.level = level;
    return dac;
  }

  public static DataAccessContext parse(String b64) {
    var bytes = DECODER.decode(b64);
    var text = new String(bytes);
    var dac = new DataAccessContext();
    var splitText = text.split(":");
    dac.id = splitText[0];
    dac.cls = splitText[1];
    if (splitText.length > 2)
      dac.level = AccessLevel.fromLevel(Integer.parseInt(splitText[2]));
    if (splitText.length > 3)
      dac.vid = splitText[3];
    if (splitText.length > 4)
      dac.sig = splitText[4];
    return dac;
  }
}
