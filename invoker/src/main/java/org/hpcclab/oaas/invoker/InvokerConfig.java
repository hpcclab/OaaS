package org.hpcclab.oaas.invoker;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;

@ConfigMapping(
  prefix = "oprc.invoker",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface InvokerConfig {
  String kafka();

  String pmHost();

  String pmPort();

  @WithDefault("oaas-invoker")
  String kafkaGroup();

  @WithDefault("oaas-fn")
  String fnProvisionTopic();

  @WithDefault("oaas-cls")
  String clsProvisionTopic();

  @WithDefault("oaas-cr-hash")
  String crHashTopic();

  @WithDefault("oaas-invoker-")
  String invokeTopicPrefix();

  Url sa();

  @WithDefault("1000")
  int connectionPoolMaxSize();

  @WithDefault("3")
  int h2ConnectionPoolMaxSize();

  @WithDefault("1")
  int numOfVerticle();

  @WithDefault("2")
  int numOfInvokerVerticle();

  @WithDefault("600000")
  int invokeTimeout();

  @WithDefault("64")
  int invokeConcurrency();

  @WithDefault("5000")
  int maxInflight();

  @WithDefault("false")
  boolean useSaOnly();

  @WithDefault("false")
  boolean clusterLock();

  @WithDefault("FETCH")
  LoadAssignMode loadMode();

  @WithDefault("none")
  List<String> initClass();

  @WithDefault("false")
  boolean enableCeHeaderOffload();

  @WithDefault("false")
  boolean enableInvReqMetric();

  @WithDefault("false")
  boolean disableVertxForGrpc();

  @WithDefault("3")
  int syncMaxRetry();

  @WithDefault("500")
  int syncRetryBackOff();

  @WithDefault("10000")
  int syncMaxRetryBackOff();

  @WithDefault("1000")
  int connectTimeout();

  @WithDefault("true")
  boolean enableWarmClsRegistry();

  @WithDefault("true")
  boolean enableWarmHashCache();

  @WithDefault("false")
  boolean forceInvokeLocal();


  @WithDefault("false")
  boolean disableHashAware();

  @WithDefault("false")
  boolean useRepForHash();

  @WithDefault("false")
  boolean useVertxGrpc();

  @WithDefault("false")
  boolean enableErrorLogging();

  enum LoadAssignMode {
    FETCH, ENV, DISABLED
  }

  interface Url {
    String url();
  }
}
