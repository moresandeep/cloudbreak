package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "AzureEnvironmentV1Parameters")
public class AzureEnvironmentParameters {

    private AzureEnvironmentParameters(AzureEnvironmentParameters.Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public AzureEnvironmentParameters build() {
            return new AzureEnvironmentParameters(this);
        }
    }
}
