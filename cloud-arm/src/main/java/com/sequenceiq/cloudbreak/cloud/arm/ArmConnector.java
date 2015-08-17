package com.sequenceiq.cloudbreak.cloud.arm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;

@Service
public class ArmConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmConnector.class);
    private static final String SSH_USER = "cloudbreak";

    @Inject
    private ArmClient armClient;
    @Inject
    private ArmResourceConnector armResourceConnector;
    @Inject
    private ArmInstanceConnector armInstanceConnector;
    @Inject
    private ArmCredentialConnector armCredentialConnector;

    @Inject
    private ArmSetup armSetup;
    @Inject
    private ArmAuthenticator armAuthenticator;

    @Override
    public String platform() {
        return ArmConstants.AZURE_RM;
    }

    @Override
    public String sshUser() {
        return SSH_USER;
    }

    @Override
    public Authenticator authentication() {
        return armAuthenticator;
    }

    @Override
    public ResourceConnector resources() {
        return armResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return armInstanceConnector;
    }

    @Override
    public Setup setup() {
        return armSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return armCredentialConnector;
    }

}
