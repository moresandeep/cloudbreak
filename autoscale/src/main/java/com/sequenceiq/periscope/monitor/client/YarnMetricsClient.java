package com.sequenceiq.periscope.monitor.client;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.CloudInstanceType;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request.HostGroupInstanceType;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.service.configuration.CloudInstanceTypeService;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component
public class YarnMetricsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnMetricsClient.class);

    private static final String YARN_API_URL = "%s/proxy/%s/resourcemanager/v1/cluster/scaling";

    private static final String HEADER_ACTOR_CRN = "x-cdp-actor-crn";

    private static final String PARAM_UPSCALE_FACTOR_NODE_RESOURCE_TYPE = "upscaling-factor-in-node-resource-types";

    private static final String DEFAULT_UPSCALE_RESOURCE_TYPE = "memory-mb";

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Inject
    private CloudInstanceTypeService cloudInstanceTypeService;

    @Inject
    private RequestLogging requestLogging;

    public YarnScalingServiceV1Response getYarnMetricsForCluster(Cluster cluster,
            String hostGroupInstanceType,
            CloudPlatform cloudPlatform) throws Exception {

        TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
        Optional<String> clusterProxyUrl = clusterProxyConfigurationService.getClusterProxyUrl();
        if (!clusterProxyUrl.isPresent() || !cluster.getTunnel().useClusterProxy()) {
            String msg = String.format("ClusterProxy Not Configured for Cluster {}, cannot query YARN Metrics.", cluster.getStackCrn());
            throw new RuntimeException(msg);
        }

        Client restClient = RestClientUtil.createClient(tlsConfig.getServerCert(),
                tlsConfig.getClientCert(), tlsConfig.getClientKey(), true);

        String yarnApiUrl = String.format(YARN_API_URL, clusterProxyUrl.get(), cluster.getStackCrn());
        YarnScalingServiceV1Request yarnScalingServiceV1Request = new YarnScalingServiceV1Request();

        CloudInstanceType cloudInstanceType = cloudInstanceTypeService.getCloudVMInstanceType(cloudPlatform, hostGroupInstanceType)
                .orElseThrow(() -> new RuntimeException(String.format("CloudVmType not found for CloudPlatform %s, " +
                        " InstanceType %s, Cluster %s ", cloudPlatform, hostGroupInstanceType, cluster.getStackCrn())));

        yarnScalingServiceV1Request.setInstanceTypes(List.of(
                new HostGroupInstanceType(cloudInstanceType.getInstanceName(),
                        cloudInstanceType.getMemoryInMB(), cloudInstanceType.getCoreCPU())));

        String clusterCreatorCrn = cluster.getClusterPertain().getUserCrn();

        YarnScalingServiceV1Response yarnResponse = requestLogging.logResponseTime(() -> {
            return restClient.target(yarnApiUrl)
                    .queryParam(PARAM_UPSCALE_FACTOR_NODE_RESOURCE_TYPE, DEFAULT_UPSCALE_RESOURCE_TYPE)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .header(HEADER_ACTOR_CRN, clusterCreatorCrn)
                    .post(Entity.json(yarnScalingServiceV1Request), YarnScalingServiceV1Response.class);
        }, String.format("YarnScalingAPI query for cluster crn '%s'", cluster.getStackCrn()));

        LOGGER.info("YarnScalingAPI reponse for cluster crn '{}',  response '{}'", cluster.getStackCrn(), yarnResponse);
        return yarnResponse;
    }
}