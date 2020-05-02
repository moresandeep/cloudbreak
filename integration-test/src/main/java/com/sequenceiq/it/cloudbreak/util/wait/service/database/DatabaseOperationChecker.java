package com.sequenceiq.it.cloudbreak.util.wait.service.database;

import static com.sequenceiq.redbeams.api.model.common.Status.CREATE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_REQUESTED;
import static com.sequenceiq.redbeams.api.model.common.Status.PRE_DELETE_IN_PROGRESS;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

public class DatabaseOperationChecker<T extends DatabaseWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String crn = waitObject.getCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            if (database == null) {
                throw new TestFailException(String.format("'%s' database was not found.", crn));
            }
            String name = database.getName();
            Status status = database.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' database. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (isDeletionInProgress(status) || status == DELETE_COMPLETED) {
                LOGGER.error("Database '{}' '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Database '%s' '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name, crn,
                        status));
            }
            if (waitObject.isFailed(status)) {
                LOGGER.error("Database '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Database '%s' '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, crn, status, database.getStatusReason()));
            }
            if (desiredStatus.equals(status)) {
                return true;
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Database has been failed. Also failed to get database status: ")
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
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            if (database == null) {
                throw new TestFailException(String.format("'%s' database was not found.", crn));
            }
            String name = database.getName();
            Status status = database.getStatus();
            throw new TestFailException(String.format("Wait operation timed out, database '%s' '%s' has been failed. Database status: '%s' "
                    + "statusReason: '%s'", name, crn, status, database.getStatusReason()));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, database has been failed. Also failed to get database status: ")
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
        return String.format("Wait operation was successfully done. '%s' database is in the desired state '%s'",
                waitObject.getCrn(), waitObject.getDesiredStatus());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response database = waitObject.getEndpoint().getByCrn(crn);
            if (database == null) {
                LOGGER.info("'{}' database was not found. Exit waiting!", crn);
                return false;
            }
            Status status = database.getStatus();
            if (status == CREATE_FAILED) {
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
        return Map.of("status", waitObject.getEndpoint().getByCrn(waitObject.getCrn()).getStatus().name());
    }

    private boolean isDeletionInProgress(Status databaseStatus) {
        Collection<Status> deleteInProgressStatuses = List.of(DELETE_REQUESTED, PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS);
        return deleteInProgressStatuses.contains(databaseStatus);
    }
}
