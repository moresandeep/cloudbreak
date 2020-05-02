package com.sequenceiq.it.cloudbreak.util.wait.service.database;

import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_FAILED;

import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

public class DatabaseTerminationChecker<T extends DatabaseWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String crn = waitObject.getCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            String name = database.getName();
            Status status = database.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' database. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (database.getStatus() == DELETE_FAILED) {
                LOGGER.error("Database '{}' '{}' termination failed (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Database '%s' '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, crn, status, database.getStatusReason()));
            }
            if (!status.equals(DELETE_COMPLETED)) {
                return false;
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Database termination failed: ")
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
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            throw new TestFailException(String.format("Wait operation timed out, '%s' '%s' database termination failed. Database status: '%s' " +
                    "statusReason: '%s'", database.getName(), crn, database.getStatus(), database.getStatusReason()));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, database termination failed. Also failed to get database status: ")
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
        return String.format("'%s' database termination successfully finished.", waitObject.getCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            Status status = database.getStatus();
            if (status == DELETE_FAILED) {
                return false;
            }
            return waitObject.isFailed(status);
        } catch (ProcessingException clientException) {
            String builder = "Exit waiting! Failed to get database due to API client exception: " +
                    System.lineSeparator() +
                    clientException.getMessage() +
                    System.lineSeparator() +
                    clientException;
            LOGGER.error(builder);
        } catch (Exception e) {
            String builder = "Exit waiting! Failed to get database, because of: " +
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
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            return Map.of("status", database.getStatus().name());
        } catch (Exception e) {
            LOGGER.warn("No database found with crn '{}'! It has been deleted successfully.", crn, e);
            return Map.of("status", DELETE_COMPLETED.name());
        }
    }
}
