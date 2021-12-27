package org.hpcclab.oaas.model.proto;

import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.object.*;
import org.hpcclab.oaas.model.provision.JobProvisionConfig;
import org.hpcclab.oaas.model.provision.KnativeProvision;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
  schemaPackageName = "org.hpcclab.oaas.model.proto",
  includeClasses = {
    UUIDAdapter.class,
    MapAdapter.class,
    MapAdapter.Entry.class,
    OaasClass.class,
    OaasFunctionBinding.class,
    StateSpecification.class,
    OaasFunction.class,
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
    OaasObject.class,
    OaasObjectOrigin.class,
    ObjectAccessModifier.class,
    OaasObjectType.class,
    OaasObjectState.class,
    OaasObjectState.StateType.class,
    OaasCompoundMember.class,
    TaskCompletion.class,
    TaskStatus.class,
    TaskState.class,
  }
)
public interface OaasSchema extends GeneratedSchema {
}
