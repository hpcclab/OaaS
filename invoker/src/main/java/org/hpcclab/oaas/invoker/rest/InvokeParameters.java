package org.hpcclab.oaas.invoker.rest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import org.hpcclab.oaas.model.invocation.InvocationResponse;

import java.util.Objects;

/**
 * @author Pawissanutt
 */
public final class InvokeParameters {
  @QueryParam("_async")
  boolean async;
  @QueryParam("_showMain")
  boolean showMain;
  @QueryParam("_showStat")
  boolean showStat;
  @QueryParam("_showOutput")
  @DefaultValue("true")
  boolean showOutput;
  @QueryParam("_showAll")
  boolean showAll;


  public InvocationResponse filter(InvocationResponse response) {
    if (showAll) return response;
    var builder = response.toBuilder();
    if (!showMain)
      builder.main(null);
    if (!showStat)
      builder.stats(null);
    if (!showOutput)
      builder.output(null);
    return builder
      .build();
  }


  @Override
  public boolean equals(Object obj) {
    if (obj==this) return true;
    if (obj==null || obj.getClass()!=this.getClass()) return false;
    var that = (InvokeParameters) obj;
    return this.async==that.async &&
      this.showMain==that.showMain &&
      this.showStat==that.showStat &&
      this.showOutput==that.showOutput;
  }

  @Override
  public int hashCode() {
    return Objects.hash(async, showMain, showStat, showOutput);
  }

  @Override
  public String toString() {
    return "InvokeParameters[" +
      "async=" + async + ", " +
      "showMain=" + showMain + ", " +
      "showStat=" + showStat + ", " +
      "showOutput=" + showOutput + ']';
  }

}
