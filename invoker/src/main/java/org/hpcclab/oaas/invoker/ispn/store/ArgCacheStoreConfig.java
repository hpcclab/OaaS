package org.hpcclab.oaas.invoker.ispn.store;

import org.hpcclab.oaas.model.HasKey;
import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSerializer;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.parsing.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ConfigurationFor(ArgCacheStore.class)
@BuiltBy(ArgCacheStoreConfig.Builder.class)
public class ArgCacheStoreConfig extends AbstractStoreConfiguration<ArgCacheStoreConfig> {
  private static final Logger logger = LoggerFactory.getLogger(ArgCacheStoreConfig.class);

  public static final AttributeDefinition<Class> VALUE_CLASS = AttributeDefinition.builder(
      "valueCls", HasKey.class, Class.class)
    .serializer(AttributeSerializer.CLASS_NAME)
    .immutable().build();
  public static final AttributeDefinition<ConnectionFactory> CONNECTION_FACTORY = AttributeDefinition.builder("connectionFactory", null, ConnectionFactory.class)
    .copier(f -> f)
    .build();


  public ArgCacheStoreConfig(AttributeSet attributes, AsyncStoreConfiguration async) {
    super(Element.STORE, attributes, async);
  }

  public static AttributeSet attributeDefinitionSet() {
    return new AttributeSet(ArgCacheStoreConfig.class, AbstractStoreConfiguration.attributeDefinitionSet(), VALUE_CLASS, CONNECTION_FACTORY);
  }

  public Class getValueCls() {
    return attributes.attribute(VALUE_CLASS)
      .get();
  }
  public ConnectionFactory getConnectionFactory() {
    return attributes.attribute(CONNECTION_FACTORY)
      .get();
  }

  public static class Builder extends AbstractStoreConfigurationBuilder<ArgCacheStoreConfig, Builder> {

    public Builder(
      PersistenceConfigurationBuilder builder) {
      super(builder, ArgCacheStoreConfig.attributeDefinitionSet());
    }


    @Override
    public ArgCacheStoreConfig create() {
      return new ArgCacheStoreConfig(
        super.attributes.protect(),
        super.async.create()
      );
    }

    @Override
    public Builder self() {
      return this;
    }

    public Builder valueCls(Class klass) {
      attributes.attribute(VALUE_CLASS).set(klass);
      return this;
    }

    public Builder connectionFactory(ConnectionFactory connectionFactory){
      attributes.attribute(CONNECTION_FACTORY).set(connectionFactory);
      return this;
    }
  }
}
