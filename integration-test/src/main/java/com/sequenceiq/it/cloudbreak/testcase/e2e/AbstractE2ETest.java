package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThat;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.testng.annotations.BeforeClass;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public abstract class AbstractE2ETest extends AbstractIntegrationTest {

    @Inject
    private TestParameter testParameter;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    protected void useSpotInstancesOnAws(Boolean useSpotInstancesOnAws) {
        testParameter.put(AwsCloudProvider.USE_SPOT_INSTANCES_KEY, useSpotInstancesOnAws.toString());
    }

    @BeforeClass
    public void resetSpot() {
        useSpotInstancesOnAws(Boolean.FALSE);
    }

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        initializeDefaultBlueprints(testContext);
    }

    /**
     * Checks the cloud platform when a specific cloud provider is the only one which is needed.
     * It throws {@link AssertionError} if the cloud provider doesn't match.
     *
     * @param cloudPlatform the required/expected cloud platform. Must not be null.
     */
    protected void checkCloudPlatform(@Nonnull CloudPlatform cloudPlatform) {
        String cloudProvider = commonCloudProperties().getCloudProvider();
        assertThat(
                String.format("The only supported cloud provider for this test is [%s]. Actual cloud provider is [%s].",
                        cloudPlatform.name(), cloudProvider),
                cloudProvider, equalToIgnoringCase(cloudPlatform.name()));
    }

}
