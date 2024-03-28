package org.hpcclab.oaas.invoker.ispn;

import com.google.protobuf.CodedInputStream;
import org.hpcclab.oaas.proto.ProtoOObject;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class ProtoOObjectMarshaller extends AbstractMarshaller {

  @Override
  protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException, InterruptedException {
    if (o instanceof ProtoOObject message) {
      return ByteBufferImpl.create(message.toByteArray());
    } else {
      throw new IllegalArgumentException("expect protobuf object");
    }
  }

  @Override
  public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
    return ProtoOObject.parseFrom(CodedInputStream.newInstance(buf, offset, length));
  }

  @Override
  public boolean isMarshallable(Object o) throws Exception {
    return o instanceof ProtoOObject;
  }

  @Override
  public MediaType mediaType() {
    return MediaType.APPLICATION_PROTOSTREAM;
  }
}
