package com.sequenceiq.cloudbreak.cloud.notification;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;
import reactor.rx.Promises;

@Component
public class ResourceNotifier implements PersistenceNotifier<ResourcePersisted> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNotifier.class);

    @Inject
    private EventBus eventBus;

    @Override
    public Promise<ResourcePersisted> notifyAllocation(CloudResource cloudResource, CloudContext cloudContext) {
        Promise<ResourcePersisted> promise = Promises.prepare();
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext.getStackId(), promise, true);
        LOGGER.info("Sending resource allocation notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", Event.wrap(notification));
        return promise;
    }

    @Override
    public Promise<ResourcePersisted> notifyDeletion(CloudResource cloudResource, CloudContext cloudContext) {
        Promise<ResourcePersisted> promise = Promises.prepare();
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext.getStackId(), promise, false);
        LOGGER.info("Sending resource deletion notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", Event.wrap(notification));
        return promise;
    }
}
