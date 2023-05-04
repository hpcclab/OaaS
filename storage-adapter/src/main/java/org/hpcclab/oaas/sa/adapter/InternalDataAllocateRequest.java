package org.hpcclab.oaas.sa.adapter;


import java.util.List;

public record InternalDataAllocateRequest(
  String oid,
  String vid,
  List<String> keys,
  String provider,
  boolean publicUrl) {
}
