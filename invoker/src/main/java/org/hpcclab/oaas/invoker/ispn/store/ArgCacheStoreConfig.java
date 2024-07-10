package org.hpcclab.oaas.invoker.ispn.store;

import org.hpcclab.oaas.invoker.ispn.GJValueMapper;
import org.hpcclab.oaas.model.HasKey;
import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSerializer;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.serializing.SerializedWith;

@ConfigurationFor(ArgCacheStore.class)
@BuiltBy(ArgCacheStoreConfigBuilder.class)
@SerializedWith(ArgStoreSerializer.class)
public class ArgCacheStoreConfig extends AbstractStoreConfiguration<ArgCacheStoreConfig> {
  public static final AttributeDefinition<Class> VALUE_CLASS = AttributeDefinition.builder(
      ArgAttribute.VALUE_CLASS.getLocalName(), HasKey.class, Class.class)
    .serializer(AttributeSerializer.CLASS_NAME)
    .immutable().build();
  public static final AttributeDefinition<Class> STORE_CLASS = AttributeDefinition.builder(
      ArgAttribute.STORE_CLASS.getLocalName(), HasKey.class, Class.class)
    .serializer(AttributeSerializer.CLASS_NAME)
    .immutable().build();
  public static final AttributeDefinition<Class> VALUE_MAPPER = AttributeDefinition.builder(
      ArgAttribute.VALUE_MAPPER.getLocalName(), GJValueMapper.class, Class.class)
    .serializer(AttributeSerializer.CLASS_NAME)
    .immutable()
    .build();
  public static final AttributeDefinition<String> DATASTORE_CONF_NAME = AttributeDefinition.builder(
      ArgAttribute.DATASTORE_CONF_NAME.getLocalName(), "DEFAULT", String.class)
    .immutable()
    .build();
  public static final AttributeDefinition<Boolean> AUTO_CREATE = AttributeDefinition.builder(
      ArgAttribute.AUTO_CREATE.getLocalName(), Boolean.FALSE, Boolean.class)
    .immutable()
    .build();


  public ArgCacheStoreConfig(AttributeSet attributes, AsyncStoreConfiguration async) {
    super(ArgAttribute.ARG_STORE, attributes, async);
  }


  public static AttributeSet attributeDefinitionSet() {
    return new AttributeSet(
      ArgCacheStoreConfig.class,
      AbstractStoreConfiguration.attributeDefinitionSet(),
      VALUE_CLASS,
      STORE_CLASS,
      VALUE_MAPPER,
      DATASTORE_CONF_NAME,
      AUTO_CREATE
    );
  }

  public Class getValueCls() {
    return attributes.attribute(VALUE_CLASS)
      .get();
  }

  public Class getStoreCls() {
    return attributes.attribute(STORE_CLASS)
      .get();
  }

  public Class getValueMapper() {
    return attributes.attribute(VALUE_MAPPER)
      .get();
  }

  public String getStoreConfName() {
    return attributes.attribute(DATASTORE_CONF_NAME)
      .get();
  }

  public boolean isAutoCreate() {
    return attributes.attribute(AUTO_CREATE)
      .get();
  }
}
