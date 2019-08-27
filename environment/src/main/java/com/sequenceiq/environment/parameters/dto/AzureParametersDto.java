package com.sequenceiq.environment.parameters.dto;

public class AzureParametersDto {

    private AzureParametersDto(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Builder() {
        }

        public AzureParametersDto build() {
            return new AzureParametersDto(this);
        }
    }
}
