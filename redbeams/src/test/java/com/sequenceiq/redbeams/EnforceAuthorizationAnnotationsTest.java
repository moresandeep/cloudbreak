package com.sequenceiq.redbeams;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.EnforceAuthorizationAnnotationsUtil;

public class EnforceAuthorizationAnnotationsTest {

    @Test
    public void testIfControllerClassHasProperAnnotation() {
        EnforceAuthorizationAnnotationsUtil.testIfControllerClassHasProperAnnotation();
    }

    @Test
    public void testIfControllerClassHasAuthorizationAnnotation() {
        EnforceAuthorizationAnnotationsUtil.testIfControllerClassHasAuthorizationAnnotation();
    }

    @Test
    public void testIfControllerMethodsHaveProperAuthorizationAnnotation() {
        EnforceAuthorizationAnnotationsUtil.testIfControllerMethodsHaveProperAuthorizationAnnotation();
    }

    @Test
    public void testIfRequestObjectsAreAuthorizedProperly() {
        EnforceAuthorizationAnnotationsUtil.testIfRequestObjectsAreAuthorizedProperly();
    }
}
