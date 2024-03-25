package org.hpcclab.oaas.invoker;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.hpcclab.oaas.storage.S3ConnConf;

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

  @WithDefault("200")
  int connectionPoolMaxSize();

  @WithDefault("50")
  int h2ConnectionPoolMaxSize();

  @WithDefault("1")
  int numOfVerticle();

  @WithDefault("2")
  int numOfInvokerVerticle();

  @WithDefault("600000")
  int invokeTimeout();

  @WithDefault("64")
  int invokeConcurrency();

  @WithDefault("500")
  int maxInflight();

  S3ConnConf s3();

  @WithDefault("false")
  boolean useSaOnly();

  @WithDefault("true")
  boolean respPubS3();

  @WithDefault("false")
  boolean clusterLock();

  @WithDefault("FETCH")
  LoadAssignMode loadMode();

  @WithDefault("none")
  List<String> initClass();

  @WithDefault("true")
  boolean warmHashCache();

  @WithDefault("false")
  boolean enableCeHeaderOffload();

  enum LoadAssignMode {
    FETCH, ENV, DISABLED
  }

  interface Url {
    String url();
  }
}
