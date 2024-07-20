package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Pagination <T>(long total,
                             long offset,
                             long itemCount,
                             List<T> items){

  public Pagination(Number total,
                    long start,
                    long itemCount,
                    List<T> items) {
    this(
      total == null? -1: total.longValue(),
      start,
      itemCount,
      items
    );
  }
}
