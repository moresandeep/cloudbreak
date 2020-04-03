package com.sequenceiq.authorization.service;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.RD_WRITE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.InternalServerErrorException;

import org.assertj.core.util.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizableFieldInfoObject;
import com.sequenceiq.authorization.resource.AuthorizableRequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@RunWith(MockitoJUnitRunner.class)
public class ResourceObjectPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:5678";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @Spy
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders = new ArrayList<ResourceBasedCrnProvider>();

    @InjectMocks
    private ResourceObjectPermissionChecker underTest;

    @Test
    public void testCheckPermissionsWithNormalObject() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn("");

        thrown.expectMessage("Request object should be an instance of AuthorizableRequestObject");
        thrown.expect(InternalServerErrorException.class);

        underTest.populateResourceBasedCrnProviderMap();
        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.ENVIRONMENT, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
    }

    @Test
    public void testCheckPermissionsWithAuthorizableRequestObject() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new AuthorizableRequestObject());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        when(resourceBasedCrnProvider.getResourceType()).thenReturn(CREDENTIAL);
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);

        underTest.populateResourceBasedCrnProviderMap();
        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.ENVIRONMENT, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(CREDENTIAL),
                eq(RD_WRITE), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    private CheckPermissionByResourceObject getAnnotation() {
        return new CheckPermissionByResourceObject() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceObject.class;
            }
        };
    }

    private static class AuthorizableRequestObject implements com.sequenceiq.authorization.resource.AuthorizableRequestObject {
        private String field = "resource";

        @Override
        public Set<AuthorizableFieldInfoObject> authorizableFieldInfos() {
            AuthorizableFieldInfoObject fieldInfo = new AuthorizableFieldInfoObject(field, CREDENTIAL, RD_WRITE, NAME);
            Set<AuthorizableFieldInfoObject> authorizableFieldInfoObjects = Sets.newHashSet();
            authorizableFieldInfoObjects.add(fieldInfo);
            return authorizableFieldInfoObjects;
        }
    }
}
