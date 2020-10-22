package com.sequenceiq.cloudbreak.audit.model;

public enum AuditEventName {
    CREATE_DATAHUB_CLUSTER,
    DELETE_DATAHUB_CLUSTER,
    STOP_DATAHUB_CLUSTER,
    RESIZE_DATAHUB_CLUSTER,
    MANUAL_REPAIR_DATAHUB_CLUSTER,
    SYNC_DATAHUB_CLUSTER,
    RETRY_DATAHUB_CLUSTER,
    INSTANCE_DELETE_DATAHUB_CLUSTER,
    MAINTAIN_DATAHUB_CLUSTER,
    START_DATAHUB_CLUSTER,
    ROTATE_DATAHUB_CLUSTER_CERTIFICATES,

    CREATE_DATALAKE_CLUSTER,
    DELETE_DATALAKE_CLUSTER,
    STOP_DATALAKE_CLUSTER,
    RESIZE_DATALAKE_CLUSTER,
    MANUAL_REPAIR_DATALAKE_CLUSTER,
    SYNC_DATALAKE_CLUSTER,
    RETRY_DATALAKE_CLUSTER,
    INSTANCE_DELETE_DATALAKE_CLUSTER,
    MAINTAIN_DATALAKE_CLUSTER,
    START_DATALAKE_CLUSTER,
    ROTATE_DATALAKE_CLUSTER_CERTIFICATES,

    CREATE_ENVIRONMENT,
    DELETE_ENVIRONMENT,
    STOP_ENVIRONMENT,
    START_ENVIRONMENT,

    CREATE_CREDENTIAL,
    DELETE_CREDENTIAL,
    MODIFY_CREDENTIAL,

    CREATE_FREEIPA,
    DELETE_FREEIPA,
    STOP_FREEIPA,
    START_FREEIPA,

    CREATE_KERBEROS_CONFIG,
    DELETE_KERBEROS_CONFIG,

    CREATE_LDAP_CONFIG,
    DELETE_LDAP_CONFIG
}
