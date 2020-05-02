package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;

public class CloudbreakWaitObject {

    private final CloudbreakClient client;

    private final String name;

    private final Map<String, Status> desiredStatuses;

    public CloudbreakWaitObject(CloudbreakClient client, String name, Map<String, Status> desiredStatuses) {
        this.client = client;
        this.name = name;
        this.desiredStatuses = desiredStatuses;
    }

    public DistroXV1Endpoint getDistroxEndpoint() {
        return client.getCloudbreakClient().distroXV1Endpoint();
    }

    public StackV4Endpoint getStackEndpoint() {
        return client.getCloudbreakClient().stackV4Endpoint();
    }

    public Long getWorkspaceId() {
        return client.getWorkspaceId();
    }

    public String getName() {
        return name;
    }

    public Map<String, Status> getDesiredStatuses() {
        return desiredStatuses;
    }

    public boolean isFailed(Map<String, Status> distroxStatuses) {
        List<Status> actualStatuses = new ArrayList<>(distroxStatuses.values());
        List<Status> failedStatuses = List.of(UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED,
                EXTERNAL_DATABASE_CREATION_FAILED, EXTERNAL_DATABASE_DELETION_FAILED);
        return failedStatuses.containsAll(actualStatuses);
    }
}
