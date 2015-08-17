package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.BuildStackFailureException;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Component
@Qualifier("stackFailureHandlerService")
public class StackFailureHandlerService implements FailureHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackFailureHandlerService.class);
    private static final double ONE_HUNDRED = 100.0;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private ResourceRepository resourceRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Inject
    private CloudbreakEventService eventService;

    @Override
    public void handleFailure(Stack stack, List<ResourceRequestResult> failedResourceRequestResults) {
        Stack localStack = stackRepository.findById(stack.getId());
        if (localStack.getFailurePolicy() == null) {
            if (failedResourceRequestResults.size() > 0) {
                LOGGER.info("Failure policy is null so error will throw");
                throwError(localStack, failedResourceRequestResults);
            }
        } else {
            switch (localStack.getFailurePolicy().getAdjustmentType()) {
            case EXACT:
                if (localStack.getFailurePolicy().getThreshold() > localStack.getFullNodeCount() - failedResourceRequestResults.size()) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(localStack, failedResourceRequestResults);
                } else if (failedResourceRequestResults.size() != 0) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(localStack, failedResourceRequestResults);
                }
                break;
            case PERCENTAGE:
                if (Double.valueOf(localStack.getFailurePolicy().getThreshold()) > calculatePercentage(failedResourceRequestResults, localStack)) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(localStack, failedResourceRequestResults);
                } else if (failedResourceRequestResults.size() != 0) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(localStack, failedResourceRequestResults);
                }
                break;
            case BEST_EFFORT:
                LOGGER.info("Decrease node counts because threshold was higher");
                handleExceptions(localStack, failedResourceRequestResults);
                break;
            default:
                LOGGER.info("Unsupported adjustment type so error will throw");
                throwError(localStack, failedResourceRequestResults);
                break;
            }
        }
    }

    private double calculatePercentage(List<ResourceRequestResult> failedResourceRequestResults, Stack localStack) {
        return Double.valueOf((localStack.getFullNodeCount() + failedResourceRequestResults.size()) / localStack.getFullNodeCount()) * ONE_HUNDRED;
    }

    private void handleExceptions(Stack stack, List<ResourceRequestResult> failedResourceRequestResult) {
        for (ResourceRequestResult exception : failedResourceRequestResult) {
            List<Resource> resourceList = new ArrayList<>();
            LOGGER.error("Error was occurred which is: " + exception.getException().orNull().getMessage(), exception.getException());
            resourceList.addAll(collectFailedResources(stack.getId(), exception.getResources()));
            resourceList.addAll(collectFailedResources(stack.getId(), exception.getBuiltResources()));
            if (!resourceList.isEmpty()) {
                LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resourceList.size());
                doRollbackAndDecreaseNodeCount(exception.getInstanceGroup(), stack, resourceList, failedResourceRequestResult);
            }
        }
    }

    private List<Resource> collectFailedResources(Long stackId, List<Resource> resources) {
        List<Resource> resourceList = new ArrayList<>();
        for (Resource resource : resources) {
            Resource newResource = resourceRepository.findByStackIdAndNameAndType(stackId, resource.getResourceName(), resource.getResourceType());
            if (newResource != null) {
                LOGGER.info(String.format("Resource %s with id %s and type %s was not deleted so added to rollback list.",
                        newResource.getResourceName(), newResource.getId(), newResource.getResourceType()));
                resourceList.add(newResource);
            }
        }
        return resourceList;
    }

    private void doRollbackAndDecreaseNodeCount(InstanceGroup ig, Stack stack, List<Resource> resourcesForDeletion, List<ResourceRequestResult> requestResult) {
        InstanceGroup instanceGroup = instanceGroupRepository.findOne(ig.getId());
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - 1);
        LOGGER.info(String.format("InstanceGroup %s node count decreased with one so the new node size is: %s",
                instanceGroup.getGroupName(), instanceGroup.getNodeCount()));
        if (instanceGroup.getNodeCount() <= 0) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will throw");
            throwError(stack, requestResult);
        } else {
            LOGGER.info("InstanceGroup saving with the new node count which is: " + instanceGroup.getNodeCount());
            instanceGroupRepository.save(instanceGroup);
            CloudPlatform cloudPlatform = stack.cloudPlatform();
            ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
            try {
                final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
                List<ResourceBuilder> resourceBuilders = instanceBuilders.get(cloudPlatform);
                for (int i = resourceBuilders.size() - 1; i >= 0; i--) {
                    ResourceBuilder resourceBuilder = resourceBuilders.get(i);
                    ResourceType resourceType = resourceBuilder.resourceType();
                    for (Resource tmpResource : resourcesForDeletion) {
                        if (resourceType.equals(tmpResource.getResourceType())) {
                            String message = String.format("Resource will be rolled back because provision failed on the resource: %s",
                                    tmpResource.getResourceName());
                            eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
                            LOGGER.info(message);
                            resourceBuilder.delete(tmpResource, dCO, stack.getRegion());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("Resource can not be deleted. Reason: {} ", e.getMessage());
            }
            for (Resource tmpResource : resourcesForDeletion) {
                LOGGER.info("Deleting resource. id: {}, name: {}, type: {}", tmpResource.getId(), tmpResource.getResourceName(), tmpResource.getResourceType());
                resourceRepository.delete(tmpResource.getId());
            }
        }
    }

    private void throwError(Stack stack, List<ResourceRequestResult> requestResult) {
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                "All resource will be rolled back because too much resource creation failed during the provisioning.");
        Stack oneWithLists = stackRepository.findOneWithLists(stack.getId());
        StringBuilder sb = new StringBuilder();
        for (ResourceRequestResult resourceRequestResult : requestResult) {
            if (resourceRequestResult.getException().orNull() != null) {
                sb.append(String.format("%s, ", resourceRequestResult.getException().orNull().getMessage()));
            }
        }
        throw new BuildStackFailureException(sb.toString(), requestResult.get(0).getException().orNull(), oneWithLists.getResources());
    }
}
