package org.hpcclab.oaas.crm;

public enum CrComponent {
  LOADBALANCER("lb"),
  INVOKER("invoker"),
  STORAGE_ADAPTER("storage-adapter"),
  CONFIG("config");

  final String svc;

  CrComponent(String svc) {
    this.svc = svc;
  }

  public String getSvc() {
    return svc;
  }
}
