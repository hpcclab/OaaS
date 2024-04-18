package org.hpcclab.oaas.crm;

public enum OprcComponent {
  LOADBALANCER("lb"),
  INVOKER("invoker"),
  STORAGE_ADAPTER("storage-adapter");

  final String svc;

  OprcComponent(String svc) {
    this.svc = svc;
  }

  public String getSvc() {
    return svc;
  }
}
