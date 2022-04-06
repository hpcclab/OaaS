package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Pagination <T>{
  long total = -1;
  long offset = -1;
  long itemCount = -1;
  List<T> items;

  public Pagination(long total, long start, long itemCount, List<T> items) {
    this.total = total;
    this.offset = start;
    this.itemCount = itemCount;
    this.items = items;
  }
}
