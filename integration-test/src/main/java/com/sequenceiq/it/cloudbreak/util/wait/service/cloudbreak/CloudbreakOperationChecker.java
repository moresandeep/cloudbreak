package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.PRE_DELETE_IN_PROGRESS;

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

public class CloudbreakOperationChecker<T extends CloudbreakWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        Map<String, Status> desiredStatuses = waitObject.getDesiredStatuses();
        try {
            StackV4Response distrox = waitObject.getDistroxEndpoint().getByName(name, Collections.emptySet());
            if (distrox == null) {
                throw new TestFailException(String.format("'%s' datahub was not found.", name));
            }
            Map<String, Status> actualStatuses = Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatus(), "clusterStatus", distrox.getStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatusReason(), "clusterStatusReason", distrox.getStatusReason());
            LOGGER.info("Waiting for the '{}' state of '{}' datahub. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (isDeletionInProgress(actualStatuses) || isDeleted(actualStatuses)) {
                LOGGER.error("Datahub '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Datahub '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name,
                        actualStatuses));
            }
            if (waitObject.isFailed(actualStatuses)) {
                LOGGER.error("Datahub '{}' is in failed state (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Datahub '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
//            desiredStatuses.entrySet().stream()
//                    .allMatch(e -> e.getValue().equals(actualStatuses.get(e.getKey())));
            if (desiredStatuses.equals(actualStatuses)) {
                LOGGER.info("Datahub '{}' is in desired state (status:'{}').", name, actualStatuses);
                return true;
            } else {
                LOGGER.info("Datahub '{}' is NOT in desired state (status:'{}').", name, actualStatuses);
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Datahub has been failed. Also failed to get datahub status: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            throw new TestFailException(builder.toString());
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            StackV4Response distrox = waitObject.getDistroxEndpoint().getByName(name, Collections.emptySet());
            if (distrox == null) {
                throw new TestFailException(String.format("'%s' datahub was not found.", name));
            }
            Map<String, Status> actualStatuses = Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatus(), "clusterStatus", distrox.getStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), name,
                    Collections.emptySet()).getStatusReason(), "clusterStatusReason", distrox.getStatusReason());
            throw new TestFailException(String.format("Wait operation timed out, datahub '%s' has been failed. Datahub status: '%s' "
                    + "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, datahub has been failed. Also failed to get datahub status: ")
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
        return String.format("Wait operation was successfully done. '%s' datahub is in the desired state '%s'",
                waitObject.getName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            StackV4Response distrox = waitObject.getDistroxEndpoint().getByName(name, Collections.emptySet());
            if (distrox == null) {
                LOGGER.info("'{}' datahub was not found. Exit waiting!", name);
                return false;
            }
            Map<String, Status> actualStatuses = Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), distrox.getName(),
                    Collections.emptySet()).getStatus(), "clusterStatus", distrox.getStatus());
            if (isCreateFailed(actualStatuses)) {
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
        return Map.of("status", waitObject.getStackEndpoint().get(waitObject.getWorkspaceId(), waitObject.getName(),
                Collections.emptySet()).getStatus().name(), "clusterStatus", waitObject.getDistroxEndpoint()
                .getByName(waitObject.getName(), Collections.emptySet()).getStatus().name());
    }

    private boolean isDeletionInProgress(Map<String, Status> distroxStatuses) {
        List<Status> actualStatuses = new ArrayList<>(distroxStatuses.values());
        List<Status> deleteInProgressStatuses = List.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
        return deleteInProgressStatuses.containsAll(actualStatuses);
    }

    private boolean isDeleted(Map<String, Status> distroxStatuses) {
        List<Status> actualStatuses = new ArrayList<>(distroxStatuses.values());
        List<Status> deleted = List.of(DELETE_COMPLETED);
        return actualStatuses.containsAll(deleted);
    }

    private boolean isCreateFailed(Map<String, Status> distroxStatuses) {
        List<Status> actualStatuses = new ArrayList<>(distroxStatuses.values());
        List<Status> failed = List.of(CREATE_FAILED);
        return actualStatuses.containsAll(failed);
    }
}
