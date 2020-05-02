package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_REQUESTED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.PROVISIONING_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeOperationChecker<T extends DatalakeWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        SdxClusterStatusResponse desiredStatus = waitObject.getDesiredStatus();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            if (sdx == null) {
                throw new TestFailException(String.format("'%s' datalake was not found.", name));
            }
            String crn = sdx.getCrn();
            SdxClusterStatusResponse status = sdx.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' datalake. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (isDeletionInProgress(status) || status == DELETED) {
                LOGGER.error("Datalake '{}' '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Datalake '%s' '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name, crn,
                        status));
            }
            if (waitObject.isFailed(status)) {
                LOGGER.error("Datalake '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Datalake '%s' '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, crn, status, sdx.getStatusReason()));
            }
            if (desiredStatus.equals(status)) {
                return true;
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Datalake has been failed. Also failed to get datalake status: ")
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
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            if (sdx == null) {
                throw new TestFailException(String.format("'%s' datalake was not found.", name));
            }
            String crn = sdx.getCrn();
            SdxClusterStatusResponse status = sdx.getStatus();
            throw new TestFailException(String.format("Wait operation timed out, datalake '%s' '%s' has been failed. Datalake status: '%s' "
                    + "statusReason: '%s'", name, crn, status, sdx.getStatusReason()));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, datalake has been failed. Also failed to get datalake status: ")
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
        return String.format("Wait operation was successfully done. '%s' datalake is in the desired state '%s'",
                waitObject.getName(), waitObject.getDesiredStatus());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            if (sdx == null) {
                LOGGER.info("'{}' datalake was not found. Exit waiting!", name);
                return false;
            }
            SdxClusterStatusResponse status = sdx.getStatus();
            if (status == PROVISIONING_FAILED) {
                return false;
            }
            return waitObject.isFailed(status);
        } catch (ProcessingException clientException) {
            String builder = "Exit waiting! Failed to get datalake due to API client exception: " +
                    System.lineSeparator() +
                    clientException.getMessage() +
                    System.lineSeparator() +
                    clientException;
            LOGGER.error(builder);
        } catch (Exception e) {
            String builder = "Exit waiting! Failed to get datalake, because of: " +
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
        return Map.of("status", waitObject.getEndpoint().get(waitObject.getName()).getStatus().name());
    }

    private boolean isDeletionInProgress(SdxClusterStatusResponse datalakeStatus) {
        Collection<SdxClusterStatusResponse> deleteInProgressStatuses = List.of(DELETE_REQUESTED, STACK_DELETION_IN_PROGRESS,
                EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
        return deleteInProgressStatuses.contains(datalakeStatus);
    }
}
