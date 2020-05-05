package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureResourceConnector.RESOURCE_GROUP_NAME;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

@Component
public class AzureResourceGroupNameProvider {

    public static final String RG_NAME = "resourceGroupName";

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    String getDefaultResourceGroupName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }

    public String getStackName(CloudContext cloudContext) {
        return Splitter.fixedLength(maxResourceNameLength - cloudContext.getId().toString().length())
                .splitToList(cloudContext.getName()).get(0) + cloudContext.getId();
    }

    public String getResourceGroupName(CloudContext cloudContext, CloudStack cloudStack) {
        return cloudStack.getParameters().getOrDefault(RESOURCE_GROUP_NAME, getDefaultResourceGroupName(cloudContext));
    }

    public String getResourceGroupName(CloudContext cloudContext, DatabaseStack databaseStack) {
        return databaseStack.getDatabaseServer().getParameters()
                .getOrDefault(RESOURCE_GROUP_NAME, getDefaultResourceGroupName(cloudContext)).toString();
    }

    public String getResourceGroupName(CloudContext cloudContext, DynamicModel dynamicModel) {
        return dynamicModel.getParameters().getOrDefault(RESOURCE_GROUP_NAME, getDefaultResourceGroupName(cloudContext)).toString();
    }
}
