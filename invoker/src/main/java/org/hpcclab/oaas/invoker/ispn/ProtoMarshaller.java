package org.hpcclab.oaas.invoker.ispn;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class ProtoMarshaller<T extends GeneratedMessageV3> extends AbstractMarshaller {
  private static final Logger logger = LoggerFactory.getLogger( ProtoMarshaller.class );
  final Class<T> cls;
  final Parser<T> parser;

  public ProtoMarshaller(Class<T> cls, Parser<T> parser) {
    this.cls = cls;
    this.parser = parser;
  }


  @Override
  protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException, InterruptedException {
    if (o instanceof GeneratedMessageV3 messageOrBuilder) {
      var b = ByteBufferImpl.create(messageOrBuilder.toByteArray());
//      logger.debug("objectToBuffer {}", b.getLength());
      return b;
    } else {
      throw new IllegalArgumentException("expect protobuf object");
    }
  }

  @Override
  public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
//    logger.debug("objectFromByteBuffer {}", length);
    return parser.parse(java.nio.ByteBuffer.wrap(buf, offset, length));
  }

  @Override
  public boolean isMarshallable(Object o) throws Exception {
    return cls.isInstance(o);
  }

  @Override
  public MediaType mediaType() {
    return MediaType.APPLICATION_OCTET_STREAM;
  }

  public interface Parser<T extends GeneratedMessageV3> {
    T parse(java.nio.ByteBuffer byteBuffer) throws IOException;
  }
}
