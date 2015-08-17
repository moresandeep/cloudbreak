package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.ResourcePersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;

@Component(ResourceCreateThread.NAME)
@Scope(value = "prototype")
public class ResourceCreateThread implements Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    public static final String NAME = "resourceCreateThread";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCreateThread.class);

    @Inject
    private ResourceBuilders resourceBuilders;
    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;
    @Inject
    private PollTaskFactory statusCheckFactory;
    @Inject
    private ResourcePersistenceNotifier resourceNotifier;

    private final long privateId;
    private final Group group;
    private final ResourceBuilderContext context;
    private final AuthenticatedContext auth;
    private final Image image;

    public ResourceCreateThread(long privateId, Group group, ResourceBuilderContext context, AuthenticatedContext auth, Image image) {
        this.privateId = privateId;
        this.group = group;
        this.context = context;
        this.auth = auth;
        this.image = image;
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        for (ComputeResourceBuilder builder : resourceBuilders.compute(auth.getCloudContext().getPlatform())) {
            PollGroup pollGroup = InMemoryStateStore.get(auth.getCloudContext().getStackId());
            if (pollGroup != null && CANCELLED.equals(pollGroup)) {
                break;
            }
            LOGGER.info("Building {} resources of {} instance group", builder.resourceType(), group.getName());
            List<CloudResource> buildableResources = builder.create(context, privateId, auth, group, image);
            createResource(auth, buildableResources);
            List<CloudResource> resources = builder.build(context, privateId, auth, group, image, buildableResources);
            context.addComputeResources(privateId, resources);
            PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(builder, auth, resources, context, true);
            List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
            results.addAll(pollerResult);
        }
        return new ResourceRequestResult<>(FutureResult.SUCCESS, results);
    }

    private List<CloudResource> createResource(AuthenticatedContext auth, List<CloudResource> cloudResources) throws Exception {
        for (CloudResource cloudResource : cloudResources) {
            resourceNotifier.notifyAllocation(cloudResource, auth.getCloudContext()).await();
        }
        return cloudResources;
    }

}
