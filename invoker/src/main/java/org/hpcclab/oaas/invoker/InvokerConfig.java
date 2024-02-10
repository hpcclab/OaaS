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
  @WithDefault("oaas-invoker-")
  String invokeTopicPrefix();
  Url sa();
  @WithDefault("100")
  int connectionPoolMaxSize();
  @WithDefault("10")
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
  boolean useSa();

  @WithDefault("false")
  boolean respPubS3();

  @WithDefault("false")
  boolean clusterLock();

  @WithDefault("FETCH")
  LoadAssignMode loadMode();
  @WithDefault("none")
  List<String> initClass();

  interface Url{
    String url();
  }

  enum LoadAssignMode{
    FETCH, ENV, DISABLED
  }
}
