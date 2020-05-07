package com.sequenceiq.cloudbreak.cloud.aws.conf;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VpcServiceEndpointConfigs {
    @Value("${cb.aws.vpcendpoints.gateway.enabled:true}")
    private Boolean gatewayServicesEnabled;

    @Value("${cb.aws.vpcendpoints.gateway.services:}")
    private List<String> gatewayServices;

    @Value("${cb.aws.vpcendpoints.interface.enabled:true}")
    private Boolean interfaceServicesEnabled;

    @Value("${cb.aws.vpcendpoints.interface.services:}")
    private List<String> interfaceServices;

    public Boolean getGatewayServicesEnabled() {
        return gatewayServicesEnabled;
    }

    public List<String> getGatewayServices() {
        return gatewayServices;
    }

    public Boolean getInterfaceServicesEnabled() {
        return interfaceServicesEnabled;
    }

    public List<String> getInterfaceServices() {
        return interfaceServices;
    }
}
