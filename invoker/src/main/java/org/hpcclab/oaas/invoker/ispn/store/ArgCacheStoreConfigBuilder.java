package org.hpcclab.oaas.invoker.ispn.store;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

import static org.hpcclab.oaas.invoker.ispn.store.ArgCacheStoreConfig.*;

/**
 * @author Pawissanutt
 */
public class ArgCacheStoreConfigBuilder extends AbstractStoreConfigurationBuilder<ArgCacheStoreConfig, ArgCacheStoreConfigBuilder> {

  public ArgCacheStoreConfigBuilder(
    PersistenceConfigurationBuilder builder) {
    this(builder, ArgCacheStoreConfig.attributeDefinitionSet());
  }


  public ArgCacheStoreConfigBuilder(PersistenceConfigurationBuilder builder, AttributeSet attributeSet) {
    super(builder, attributeSet);
  }


  @Override
  public ArgCacheStoreConfig create() {
    return new ArgCacheStoreConfig(
      super.attributes.protect(),
      super.async.create()
    );
  }

  @Override
  public ArgCacheStoreConfigBuilder self() {
    return this;
  }

  public ArgCacheStoreConfigBuilder valueCls(Class klass) {
    attributes.attribute(VALUE_CLASS).set(klass);
    return this;
  }

  public ArgCacheStoreConfigBuilder storeCls(Class klass) {
    attributes.attribute(STORE_CLASS).set(klass);
    return this;
  }
  public ArgCacheStoreConfigBuilder valueMapper(Class valueMapper){
    attributes.attribute(VALUE_MAPPER).set(valueMapper);
    return this;
  }

  public ArgCacheStoreConfigBuilder storeConfName(String name) {
    attributes.attribute(DATASTORE_CONF_NAME).set(name);
    return this;
  }
  public ArgCacheStoreConfigBuilder autoCreate(boolean val) {
    attributes.attribute(AUTO_CREATE).set(val);
    return this;
  }
}
