package com.sequenceiq.cloudbreak.cloud.gcp.poller;

import static com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpDatabaseBaseResourceChecker.OPERATION_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.api.services.sqladmin.SQLAdmin;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.GcpDatabaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerCheckerService;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpDatabasePollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabasePollerProvider.class);

    private final DatabaseServerCheckerService checker;

    private final GcpDatabaseResourceChecker gcpResourceChecker;

    public GcpDatabasePollerProvider(DatabaseServerCheckerService checker, GcpDatabaseResourceChecker gcpResourceChecker) {
        this.checker = checker;
        this.gcpResourceChecker = gcpResourceChecker;
    }

    public AttemptMaker<Void> launchDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.CREATED);
    }

    public AttemptMaker<Void> stopStartDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.CREATED);
    }

    public AttemptMaker<Void> insertUserPoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.CREATED);
    }

    public AttemptMaker<Void> terminateDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.DELETED);
    }

    private AttemptResult<Void> fetchOperationResults(AuthenticatedContext ac, List<CloudResource> resources, ResourceStatus expectedStatus) {
        SQLAdmin sqlAdmin = GcpStackUtil.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        List<CloudResourceStatus> cloudResourceStatuses = checkResources(
                ResourceType.GCP_DATABASE,
                sqlAdmin,
                ac,
                resources,
                expectedStatus);
        if (checkAllResourceIsInTheRightState(cloudResourceStatuses, expectedStatus)) {
            return AttemptResults.finishWith(null);
        } else if (hasFailedState(cloudResourceStatuses)) {
            return AttemptResults.breakFor(getFailedStatus(cloudResourceStatuses));
        }
        return AttemptResults.justContinue();
    }

    private boolean checkAllResourceIsInTheRightState(List<CloudResourceStatus> cloudResourceStatuses, ResourceStatus expectedStatus) {
        List<CloudResourceStatus> collect = cloudResourceStatuses.stream()
                .filter(e -> e.getStatus().equals(expectedStatus))
                .collect(Collectors.toList());
        return collect.size() == cloudResourceStatuses.size();
    }

    private List<CloudResourceStatus> getFailedStates(List<CloudResourceStatus> cloudResourceStatuses) {
        return cloudResourceStatuses.stream()
                .filter(e -> e.getStatus().equals(ResourceStatus.FAILED))
                .collect(Collectors.toList());
    }

    private boolean hasFailedState(List<CloudResourceStatus> cloudResourceStatuses) {
        return !getFailedStates(cloudResourceStatuses).isEmpty();
    }

    private String getFailedStatus(List<CloudResourceStatus> cloudResourceStatuses) {
        return getFailedStates(cloudResourceStatuses).get(0).getStatusReason();
    }

    public List<CloudResourceStatus> checkResources(
            ResourceType type, SQLAdmin sqlAdmin, AuthenticatedContext auth, Iterable<CloudResource> resources, ResourceStatus waitedStatus) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}", type, resource);
            try {
                String operationId = resource.getStringParameter(OPERATION_ID);
                com.google.api.services.sqladmin.model.Operation operation = gcpResourceChecker.check(sqlAdmin, auth, operationId);
                boolean finished = operation == null || GcpStackUtil.isOperationFinished(operation);
                if (finished) {
                    result.add(new CloudResourceStatus(resource, waitedStatus));
                } else {
                    result.add(new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS));
                }
            } catch (Exception e) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new GcpResourceException("Error during status check", type,
                        cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
            }
        }
        return result;
    }
}
