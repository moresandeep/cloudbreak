package com.sequenceiq.authorization.resource;

public class AuthorizableFieldInfoObject {

    private String value;

    private AuthorizationResourceType resourceType;

    private AuthorizationResourceAction resourceAction;

    private AuthorizationVariableType variableType;

    public AuthorizableFieldInfoObject(String value, AuthorizationResourceType resourceType,
            AuthorizationResourceAction resourceAction, AuthorizationVariableType variableType) {
        this.value = value;
        this.resourceType = resourceType;
        this.resourceAction = resourceAction;
        this.variableType = variableType;
    }

    public String getValue() {
        return value;
    }

    public AuthorizationResourceType getResourceType() {
        return resourceType;
    }

    public AuthorizationResourceAction getResourceAction() {
        return resourceAction;
    }

    public AuthorizationVariableType getVariableType() {
        return variableType;
    }
}
