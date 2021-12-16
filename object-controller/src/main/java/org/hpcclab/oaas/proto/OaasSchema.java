package org.hpcclab.oaas.proto;

import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.object.*;
import org.hpcclab.oaas.model.provision.JobProvisionConfig;
import org.hpcclab.oaas.model.provision.KnativeProvision;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.types.java.CommonContainerTypes;
import org.infinispan.protostream.types.java.CommonTypes;

@AutoProtoSchemaBuilder(
  schemaPackageName = "org.hpcclab.oaas.proto",
  includeClasses = {
    UUIDAdapter.class,
    MapAdapter.class,
    MapAdapter.Entry.class,
    OaasClassPb.class,
    OaasFunctionBindingPb.class,
    StateSpecification.class,
    OaasFunctionPb.class,
    OaasFunctionType.class,
    OaasFunctionValidation.class,
    FunctionAccessModifier.class,
    OaasObjectRequirement.class,
    OaasWorkflow.class,
    OaasWorkflowStep.class,
    OaasWorkflowExport.class,
    ProvisionConfig.class,
    ProvisionConfig.Type.class,
    JobProvisionConfig.class,
    KnativeProvision.class,
    OaasObjectPb.class,
    OaasObjectOrigin.class,
    ObjectAccessModifier.class,
    OaasObjectType.class,
    OaasObjectState.class,
    OaasObjectState.StateType.class,
    OaasCompoundMemberDto.class,
  },
  dependsOn = {CommonTypes.class}
)
public interface OaasSchema extends GeneratedSchema {
}
