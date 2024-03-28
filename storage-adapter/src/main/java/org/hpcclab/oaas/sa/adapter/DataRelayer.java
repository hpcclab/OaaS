package org.hpcclab.oaas.sa.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

/**
 * @author Pawissanutt
 */
public interface DataRelayer {
  Uni<Response> relay(String url);
}
