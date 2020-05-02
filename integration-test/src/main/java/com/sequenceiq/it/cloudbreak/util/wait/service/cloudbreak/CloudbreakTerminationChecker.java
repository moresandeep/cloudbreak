package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class CloudbreakTerminationChecker<T extends CloudbreakWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        Map<String, Status> desiredStatuses = waitObject.getDesiredStatuses();
        try {
            StackV4Response distrox = waitObject.getDistroxEndpoint().getByName(name, Collections.emptySet());
            Map<String, Status> actualStatuses = Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatus(), "clusterStatus", distrox.getStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatusReason(), "clusterStatusReason", distrox.getStatusReason());
            LOGGER.info("Waiting for the '{}' state of '{}' datahub. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (isDeleteFailed(actualStatuses)) {
                LOGGER.error("Datahub '{}' termination failed (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Datahub '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (isNotDeleted(actualStatuses)) {
                return false;
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Datahub termination failed: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            throw new TestFailException(builder.toString());
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            StackV4Response distrox = waitObject.getDistroxEndpoint().getByName(name, Collections.emptySet());
            Map<String, Status> actualStatuses = Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatus(), "clusterStatus", distrox.getStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatusReason(), "clusterStatusReason", distrox.getStatusReason());
            throw new TestFailException(String.format("Wait operation timed out, '%s' datahub termination failed. Datahub status: '%s' " +
                    "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, datahub termination failed. Also failed to get datahub status: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            throw new TestFailException(builder.toString());
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' datahub termination successfully finished.", waitObject.getName());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            StackV4Response distrox = waitObject.getDistroxEndpoint().getByName(name, Collections.emptySet());
            Map<String, Status> actualStatuses = Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), distrox.getName(),
                    Collections.emptySet()).getStatus(), "clusterStatus", distrox.getStatus());
            if (isDeleteFailed(actualStatuses)) {
                return false;
            }
            return waitObject.isFailed(actualStatuses);
        } catch (ProcessingException clientException) {
            String builder = "Exit waiting! Failed to get datahub due to API client exception: " +
                    System.lineSeparator() +
                    clientException.getMessage() +
                    System.lineSeparator() +
                    clientException;
            LOGGER.error(builder);
        } catch (Exception e) {
            String builder = "Exit waiting! Failed to get datahub, because of: " +
                    System.lineSeparator() +
                    e.getMessage() +
                    System.lineSeparator() +
                    e;
            LOGGER.error(builder);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        String name = waitObject.getName();
        Long workspaceId = waitObject.getWorkspaceId();
        try {
            return Map.of("status", waitObject.getStackEndpoint().get(workspaceId, name,
                    Collections.emptySet()).getStatus().name(), "clusterStatus", waitObject.getDistroxEndpoint()
                    .getByName(name, Collections.emptySet()).getStatus().name());
        } catch (Exception e) {
            LOGGER.warn("No cluster found with name '{}'! It has been deleted successfully.", name, e);
            return Map.of("status", DELETE_COMPLETED.name());
        }
    }

    private boolean isDeleteFailed(Map<String, Status> distroxStatuses) {
        List<Status> actualStatuses = new ArrayList<>(distroxStatuses.values());
        List<Status> failed = List.of(DELETE_FAILED);
        return actualStatuses.containsAll(failed);
    }

    private boolean isNotDeleted(Map<String, Status> distroxStatuses) {
        List<Status> actualStatuses = new ArrayList<>(distroxStatuses.values());
        List<Status> deleted = List.of(DELETE_COMPLETED);
        return !actualStatuses.containsAll(deleted);
    }

}
