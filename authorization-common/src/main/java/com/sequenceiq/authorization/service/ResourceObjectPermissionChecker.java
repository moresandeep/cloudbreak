package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizableRequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;

@Component
public class ResourceObjectPermissionChecker implements PermissionChecker<CheckPermissionByResourceObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceObjectPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders;

    private final Map<AuthorizationResourceType, ResourceBasedCrnProvider> resourceBasedCrnProviderMap = new HashMap<>();

    @PostConstruct
    public void populateResourceBasedCrnProviderMap() {
        resourceBasedCrnProviders.forEach(resourceBasedCrnProvider ->
                resourceBasedCrnProviderMap.put(resourceBasedCrnProvider.getResourceType(), resourceBasedCrnProvider));
    }

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        // check fields of resourceObject
        Object resourceObject = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceObject.class, Object.class);
        checkPermissionOnResourceObjectFields(userCrn, resourceObject);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
    }

    private void checkPermissionOnResourceObjectFields(String userCrn, Object resourceObject) {
        if (!(resourceObject instanceof AuthorizableRequestObject)) {
            throw new InternalServerErrorException("Request object should be an instance of AuthorizableRequestObject");
        }
        AuthorizableRequestObject requestObject = (AuthorizableRequestObject) resourceObject;
        requestObject.authorizableFieldInfos().forEach(fieldInfo -> {
            String resourceNameOrCrn = fieldInfo.getValue();
            AuthorizationResourceType resourceType = fieldInfo.getResourceType();
            AuthorizationResourceAction action = fieldInfo.getResourceAction();
            checkActionType(resourceType, action);
            String resourceCrn = fieldInfo.getVariableType().equals(AuthorizationVariableType.NAME)
                    ? resourceBasedCrnProviderMap.get(resourceType).getResourceCrnByResourceName(resourceNameOrCrn)
                    : resourceNameOrCrn;
            commonPermissionCheckingUtils.checkPermissionForUserOnResource(resourceType, action, userCrn, resourceCrn);
        });
    }

    @Override
    public Class<CheckPermissionByResourceObject> supportedAnnotation() {
        return CheckPermissionByResourceObject.class;
    }

    @Override
    public AuthorizationResourceAction.ActionType actionType() {
        return AuthorizationResourceAction.ActionType.RESOURCE_DEPENDENT;
    }
}
