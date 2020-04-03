package com.sequenceiq.authorization.resource;

import java.util.Set;

public interface AuthorizableRequestObject {
    Set<AuthorizableFieldInfoObject> authorizableFieldInfos();
}
