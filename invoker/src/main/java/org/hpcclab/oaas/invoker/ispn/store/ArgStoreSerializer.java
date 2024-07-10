package org.hpcclab.oaas.invoker.ispn.store;

import org.infinispan.commons.configuration.io.ConfigurationWriter;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.serializing.AbstractStoreSerializer;
import org.infinispan.configuration.serializing.ConfigurationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawissanutt
 */
public class ArgStoreSerializer extends AbstractStoreSerializer implements ConfigurationSerializer<ArgCacheStoreConfig> {
  private static final Logger logger = LoggerFactory.getLogger( ArgStoreSerializer.class );
  @Override
  public void serialize(ConfigurationWriter writer, ArgCacheStoreConfig configuration) {
    writer.writeStartElement(ArgAttribute.ARG_STORE);
    writer.writeDefaultNamespace(ArgStoreParser.NAMESPACE + Version.getMajorMinor());
    configuration.attributes().write(writer);
    writeCommonStoreSubAttributes(writer, configuration);
    writeCommonStoreElements(writer, configuration);
    writer.writeEndElement();
  }
}
