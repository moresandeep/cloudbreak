package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategyType;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

//import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetSelectorStrategy;
//import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetSelectorStrategyType;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"cb.max.aws.resource.name.length = 345678","cb.aws.cf.network.template.path=src/main/resources/templates/aws-cf-network.ftl"})
public class AwsNetworkConnectorTest_2 {

    @Inject
    private AwsNetworkConnector testCase;

    @SpyBean
    private AwsClient awsClient;

    @SpyBean
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @MockBean
    private CloudFormationStackUtil cfStackUtil;

    @MockBean
    private AwsPollTaskFactory awsPollTaskFactory;

    @MockBean
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @MockBean
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @MockBean
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @SpyBean
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @SpyBean
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @SpyBean
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @SpyBean
    private AwsTaggingService awsTaggingService;

    @Mock
    private DescribeStacksResult describeStacksResult;

    @Mock
    private CreateStackResult createStackResult;

    @Mock
    private DeleteStackResult deleteStackResult;

    @MockBean
    private Map<SubnetFilterStrategyType, SubnetFilterStrategy> subnetFilterStrategyMap;

    @MockBean(name = "cloudApiListeningScheduledExecutorService")
    private ListeningScheduledExecutorService cloudApiListeningScheduledExecutorService;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @Mock
    private PollTask<Boolean> pollTask;




    private Set<NetworkSubnetRequest> publicSubNets = Set.of(new NetworkSubnetRequest("1.1.1.1/8", PUBLIC), new NetworkSubnetRequest("1.1.1.2/8", PUBLIC));

    private CloudCredential credential= new CloudCredential("cloudCredId","cloudCredName");


    private List<SubnetRequest> createSubnetRequestList(){
        SubnetRequest subnetRequest1 = new SubnetRequest();
        subnetRequest1.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest1.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest1.setAvailabilityZone("az1");

        SubnetRequest subnetRequest2 = new SubnetRequest();
        subnetRequest2.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest2.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest2.setAvailabilityZone("az2");

        SubnetRequest subnetRequest3 = new SubnetRequest();
        subnetRequest3.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest3.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest3.setAvailabilityZone("az3");

        return List.of(subnetRequest1, subnetRequest2, subnetRequest3);
    }

    private NetworkDeletionRequest createNetworkDeletionRequest(Boolean exist) {
        return new NetworkDeletionRequest.Builder().
                withStackName("stackName").
                withCloudCredential(credential).
                withRegion("region").
                withResourceGroup("resourceGroup").
                withExisting(exist).
                build();
    }
    private NetworkCreationRequest createNetworkCreationRequest(){
        return new NetworkCreationRequest.Builder()
                .withCloudCredential(credential)
                .withEnvCrn("creatorCRn")
                .withEnvId(1L)
                .withEnvName("envName")
                .withPublicSubnets(publicSubNets)
                .withPrivateSubnets(publicSubNets)
                .withNetworkCidr("networkCidr")
                .withPrivateSubnetEnabled(true)
                .withRegion(Region.region("HU_WEST_1"))
                .withUserName("user@cloudera.com")
                .withNetworkCidr("0.0.0.0/16")
                .withStackName("envName-1")
                .build();
    }

    private Map<String,String> CreateOutputVpcExist() {
        Map<String, String> output = new HashMap<>();
        output.put("CreatedVpc", "VpcId");
        output.put("Created_Subnet_0", "Subnet_Id_0");
        output.put("Created_Subnet_1", "Subnet_Id_1");
        output.put("Created_Subnet_2", "Subnet_Id_2");
        return output;
    }
    private Map<String,String> CreateOutputVpcNotExist() {
        Map<String, String> output = new HashMap<>();
        return output;
    }

    @Test
    public void createNetworkWithSubnetsTimeout(){
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(),any());
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(),any(),any());
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(),any());
        doThrow(new AmazonServiceException("Failed to connect server")).when(amazonCloudFormationRetryClient).describeStacks(any());
        assertThatThrownBy(()->testCase.createNetworkWithSubnets(createNetworkCreationRequest())).isInstanceOf(CloudConnectorException.class)
                .hasMessage("Failed to create network.");
    }

    @Test
    public void createNetworkWithSubnetsAwsBackoffSyncPollingSchedulerException() throws InterruptedException, ExecutionException, TimeoutException {
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(),any());
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(),any(),any());
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(),any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(),any());
        doReturn(describeStacksResult).when(amazonCloudFormationRetryClient).describeStacks(any());
        doReturn(pollTask).when(awsPollTaskFactory).newAwsCreateNetworkStatusCheckerTask(any(),any(),any(),any(),any());
        doThrow(new InterruptedException("Process Interrupted")).when(awsBackoffSyncPollingScheduler).schedule(any(PollTask.class));
        assertThatThrownBy(()->testCase.createNetworkWithSubnets(createNetworkCreationRequest())).isInstanceOf(CloudConnectorException.class)
                .hasMessage("Process Interrupted");
    }

    @Test
    public void createNetworkWithSubnetsCreateCloudNetwork() throws InterruptedException, ExecutionException, TimeoutException {
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(),any());
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(),any(),any());
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(),any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(),any());
        doReturn(describeStacksResult).when(amazonCloudFormationRetryClient).describeStacks(any());
        doReturn(pollTask).when(awsPollTaskFactory).newAwsCreateNetworkStatusCheckerTask(any(),any(),any(),any(),any());
        doReturn(true).when(awsBackoffSyncPollingScheduler).schedule(any(PollTask.class));
        doReturn(CreateOutputVpcExist()).when(cfStackUtil).getOutputs(any(),any());
        doReturn(publicSubNets).when(awsCreatedSubnetProvider).provide(any(),any());


        CreatedCloudNetwork createdCloudNetwork=testCase.createNetworkWithSubnets(createNetworkCreationRequest());

        assertEquals(createdCloudNetwork.getNetworkId(),"VpcId");
        assertEquals(createdCloudNetwork.getStackName(), createNetworkCreationRequest().getStackName());
    }

    @Test
    public void createNetworkWithSubnetsCfStaackDoesNotExist() throws InterruptedException, ExecutionException, TimeoutException {

        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(),any());
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(),any(),any());
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(),any());
        AmazonServiceException amazonServiceException = new AmazonServiceException("Network does not exist");
        amazonServiceException.setStatusCode(400);
        doThrow(amazonServiceException).when(amazonCloudFormationRetryClient).describeStacks(any());
        doReturn(createStackResult).when(amazonCloudFormationRetryClient).createStack(any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(),any());
        doReturn(pollTask).when(awsPollTaskFactory).newAwsCreateNetworkStatusCheckerTask(any(),any(),any(),any(),any());
        doReturn(true).when(awsBackoffSyncPollingScheduler).schedule(any(PollTask.class));
        doReturn(CreateOutputVpcExist()).when(cfStackUtil).getOutputs(any(),any());
        doReturn(publicSubNets).when(awsCreatedSubnetProvider).provide(any(),any());

        CreatedCloudNetwork createdCloudNetwork=testCase.createNetworkWithSubnets(createNetworkCreationRequest());

        assertEquals(createdCloudNetwork.getNetworkId(),"VpcId");
        assertEquals(createdCloudNetwork.getStackName(), createNetworkCreationRequest().getStackName());

    }

    @Test
    public void getNetworkCidrReturnFirsCidr() {
        String existingVpc = "vpc-1";
        String cidrBlock1 = "10.0.0.0/16";
        String cidrBlock2 = "10.23.0.0/16";

        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "hu-east-1"));
        CloudCredential credential = new CloudCredential();
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(),any());
        DescribeVpcsResult describeVpcsResult = new DescribeVpcsResult().withVpcs(
                List.of(new Vpc().withCidrBlock(cidrBlock1), new Vpc().withCidrBlock(cidrBlock2)));
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("hu-east-1"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        String result = testCase.getNetworkCidr(network, credential);
        assertEquals(cidrBlock1, result);

    }

    @Test
    public void getNetworkCidrThrowsError() throws BadRequestException {
        String existingVpc = "vpc-1";
        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "hu-east-1"));
        CloudCredential credential = new CloudCredential();
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(),any());
        DescribeVpcsResult describeVpcsResult = new DescribeVpcsResult().withVpcs();
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("hu-east-1"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);
        assertThatThrownBy(()-> testCase.getNetworkCidr(network, credential)).isInstanceOf(BadRequestException.class)
                    .hasMessage("VPC cidr could not fetch from AWS: "+ existingVpc);


    }

    @Test
    public void deleteNetworkWithSubnetsRequestExists(){
        NetworkDeletionRequest networkDeletionRequest=createNetworkDeletionRequest(Boolean.TRUE);
        testCase.deleteNetworkWithSubnets(networkDeletionRequest);
        verifyNoMoreInteractions(awsClient);
    }

    @Test
    public void deleteNetworkWithSubnetsSchedulerThrowsError() throws TimeoutException, ExecutionException, InterruptedException {
        NetworkDeletionRequest networkDeletionRequest=createNetworkDeletionRequest(Boolean.FALSE);
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(),any());
        doReturn(deleteStackResult).when(amazonCloudFormationRetryClient).deleteStack(any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(AwsCredentialView.class),any());
        doReturn(pollTask).when(awsPollTaskFactory).newAwsTerminateNetworkStatusCheckerTask(any(),any(),any(),any(),any());
        doThrow(new InterruptedException("Process Interrupted")).when(awsBackoffSyncPollingScheduler).schedule(any(PollTask.class));
        assertThatThrownBy(()->testCase.deleteNetworkWithSubnets(createNetworkDeletionRequest(Boolean.FALSE))).isInstanceOf(CloudConnectorException.class)
                .hasMessage("Process Interrupted");
    }

    @Test
    public void deleteNetworkWithSubnetsSuccessfull() throws InterruptedException, ExecutionException, TimeoutException {
        NetworkDeletionRequest networkDeletionRequest=createNetworkDeletionRequest(Boolean.FALSE);
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(),any());
        doReturn(deleteStackResult).when(amazonCloudFormationRetryClient).deleteStack(any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(AwsCredentialView.class),any());
        doReturn(pollTask).when(awsPollTaskFactory).newAwsTerminateNetworkStatusCheckerTask(any(),any(),any(),any(),any());
        doReturn(true).when(awsBackoffSyncPollingScheduler).schedule(any(PollTask.class));
        testCase.deleteNetworkWithSubnets(createNetworkDeletionRequest(Boolean.FALSE));


    }


        @Configuration
    @Import({AwsSubnetRequestProvider.class,
            AwsNetworkCfTemplateProvider.class,
            AwsNetworkConnector.class,
            AwsClient.class,
            FreeMarkerConfigurationFactoryBean.class,
            FreeMarkerTemplateUtils.class,
            JsonHelper.class,
            CloudFormationStackUtil.class,
            AwsSessionCredentialClient.class,
            AwsEnvironmentVariableChecker.class,
            AwsDefaultZoneProvider.class,
            RetryService.class,
            AwsPollTaskFactory.class,
            AwsBackoffSyncPollingScheduler.class,

    })
    static class Config{

    }

}
