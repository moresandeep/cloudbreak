package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;

import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeTerminationChecker<T extends DatalakeWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        SdxClusterStatusResponse desiredStatus = waitObject.getDesiredStatus();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            String crn = sdx.getCrn();
            SdxClusterStatusResponse status = sdx.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' datalake. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (sdx.getStatus() == DELETE_FAILED) {
                LOGGER.error("Datalake '{}' '{}' termination failed (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Datalake '%s' '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, crn, status, sdx.getStatusReason()));
            }
            if (!status.equals(DELETED)) {
                return false;
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Datalake termination failed: ")
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
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            throw new TestFailException(String.format("Wait operation timed out, '%s' '%s' datalake termination failed. Datalake status: '%s' " +
                    "statusReason: '%s'", name, sdx.getCrn(), sdx.getStatus(), sdx.getStatusReason()));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, datalake termination failed. Also failed to get datalake status: ")
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
        return String.format("'%s' datalake termination successfully finished.", waitObject.getName());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            SdxClusterStatusResponse status = sdx.getStatus();
            if (status == DELETE_FAILED) {
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
        String name = waitObject.getName();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            return Map.of("status", sdx.getStatus().name());
        } catch (Exception e) {
            LOGGER.warn("No sdx found with name '{}'! It has been deleted successfully.", name, e);
            return Map.of("status", DELETED.name());
        }
    }
}
