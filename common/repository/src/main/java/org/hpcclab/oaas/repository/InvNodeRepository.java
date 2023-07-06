package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.invocation.InvocationNode;

import java.util.List;

public interface InvNodeRepository extends EntityRepository<String, InvocationNode> {
}
