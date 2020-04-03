package com.sequenceiq.environment.api.v1.credential.model.request;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.RD_READ;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;

import java.util.Set;

import org.glassfish.jersey.internal.guava.Sets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.authorization.resource.AuthorizableFieldInfoObject;
import com.sequenceiq.authorization.resource.AuthorizableRequestObject;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;

import io.swagger.annotations.ApiModel;

@ApiModel(description = CredentialDescriptor.CREDENTIAL_NOTES, parent = CredentialRequest.class, value = "CredentialV1ModifyRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CredentialModifyRequest extends CredentialRequest implements AuthorizableRequestObject {

    @Override
    public Set<AuthorizableFieldInfoObject> authorizableFieldInfos() {
        AuthorizableFieldInfoObject fieldInfo = new AuthorizableFieldInfoObject(getName(), CREDENTIAL, RD_READ, NAME);
        Set<AuthorizableFieldInfoObject> authorizableFieldInfoObjects = Sets.newHashSet();
        authorizableFieldInfoObjects.add(fieldInfo);
        return authorizableFieldInfoObjects;
    }
}
