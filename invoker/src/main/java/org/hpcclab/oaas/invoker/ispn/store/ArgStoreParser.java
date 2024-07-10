package org.hpcclab.oaas.invoker.ispn.store;

import org.infinispan.commons.configuration.io.ConfigurationReader;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hpcclab.oaas.invoker.ispn.store.ArgStoreParser.NAMESPACE;

/**
 * @author Pawissanutt
 */


@Namespace(root = "arg-store")
@Namespace(uri = NAMESPACE + "*", root = "arg-store")
public class ArgStoreParser implements ConfigurationParser {
  static final String NAMESPACE = Parser.NAMESPACE + "store:arg-store:";
  private static final Logger logger = LoggerFactory.getLogger(ArgStoreParser.class);

  @Override
  public void readElement(ConfigurationReader reader, ConfigurationBuilderHolder holder) {
    ConfigurationBuilder builder = holder.getCurrentConfigurationBuilder();
    ArgAttribute element = ArgAttribute.forName(reader.getLocalName());
    switch (element) {
      case ARG_STORE: {
        parse(reader, builder.persistence().addStore(ArgCacheStoreConfigBuilder.class));
        break;
      }
      default: {
        throw ParseUtils.unexpectedElement(reader);
      }
    }
  }

  private void parse(ConfigurationReader reader, ArgCacheStoreConfigBuilder builder) {
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      String value = reader.getAttributeValue(i);
      String attributeName = reader.getAttributeName(i);
      ArgAttribute attribute = ArgAttribute.forName(attributeName);
      try {
        switch (attribute) {
          case VALUE_CLASS -> builder.valueCls(Class.forName(value));
          case STORE_CLASS -> builder.storeCls(Class.forName(value));
          case VALUE_MAPPER -> builder.valueMapper(Class.forName(value));
          case DATASTORE_CONF_NAME -> builder.storeConfName(value);
          case AUTO_CREATE -> builder.autoCreate(Boolean.parseBoolean(value));
          default -> CacheParser.parseStoreAttribute(reader, i, builder);
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    while (reader.inTag()) {
      CacheParser.parseStoreElement(reader, builder);
    }
  }

  @Override
  public Namespace[] getNamespaces() {
    return ParseUtils.getNamespaceAnnotations(getClass());
  }
}
