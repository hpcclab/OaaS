package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.model.invocation.InvocationChain;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author Pawissanutt
 */
public class InvocationChainProcessor {
  private static final Logger logger = LoggerFactory.getLogger( InvocationChainProcessor.class );

  final InvocationQueueProducer producer;

  public InvocationChainProcessor(InvocationQueueProducer producer) {
    this.producer = producer;
  }

  Uni<InvocationCtx> handle(InvocationCtx ctx) {
    if (ctx.getRequest().chains()!=null && !ctx.getRequest().chains().isEmpty()) {
      if (ctx.getChains().isEmpty()) {
        ctx.setChains(ctx.getRequest().chains());
      } else {
        ctx.getChains().addAll(ctx.getRequest().chains());
      }
    }
    if (logger.isDebugEnabled())
      logger.debug("processing [{},{}:{}:{}] with [{}] chains",
        ctx.getRequest().invId(),
        ctx.getRequest().cls(),
        ctx.getRequest().main(),
        ctx.getRequest().fb(),
        ctx.getChains().size()
      );
    List<InvocationRequest> requests = ctx.getChains()
      .stream()
      .map(chain -> buildRequest(ctx, chain))
      .toList();
    return producer.offer(requests)
      .replaceWith(ctx);
  }

  InvocationRequest buildRequest(InvocationCtx ctx, InvocationChain chain) {
    if (ctx.getOutput()!=null &&
      Objects.equals(chain.main(), ctx.getOutput().getId())) {
      chain = chain.toBuilder().cls(ctx.getOutput().getCls())
        .build();
    }
    return InvocationRequest.builder()
      .main(chain.main())
      .cls(chain.cls())
      .fb(chain.fb())
      .body(chain.body())
      .outId(chain.outId())
      .invId(chain.invId())
      .immutable(chain.immutable())
      .args(chain.args())
      .chains(chain.chains())
      .partKey(chain.main())
      .build();
  }
}
