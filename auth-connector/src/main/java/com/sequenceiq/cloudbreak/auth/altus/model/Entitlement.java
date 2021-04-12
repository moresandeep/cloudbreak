package com.sequenceiq.cloudbreak.auth.altus.model;

public enum Entitlement {

    DATAHUB_FLOW_SCALING,
    DATAHUB_STREAMING_SCALING,
    DATAHUB_DEFAULT_SCALING,
    CDP_AZURE,
    CDP_GCP,
    CDP_BASE_IMAGE,
    CDP_AUTOMATIC_USERSYNC_POLLER,
    CDP_FREEIPA_HA_REPAIR,
    CDP_FREEIPA_BACKUP_LOCATION_CONFIG,
    CLOUDERA_INTERNAL_ACCOUNT,
    CDP_FMS_CLUSTER_PROXY,
    CDP_CLOUD_STORAGE_VALIDATION,
    CDP_CLOUD_STORAGE_VALIDATION_AWS,
    CDP_CLOUD_STORAGE_VALIDATION_AZURE,
    CDP_CLOUD_STORAGE_VALIDATION_GCP,
    CDP_CM_HA,
    CDP_RAZ,
    CDP_RAW_S3,
    CDP_MEDIUM_DUTY_SDX,
    CDP_RUNTIME_UPGRADE,
    CDP_RUNTIME_UPGRADE_DATAHUB,
    LOCAL_DEV,
    CDP_AZURE_SINGLE_RESOURCE_GROUP,
    CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT,
    CDP_CLOUD_IDENTITY_MAPPING,
    CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE,
    CDP_SDX_HBASE_CLOUD_STORAGE,
    CDP_DATA_LAKE_AWS_EFS,
    CDP_DATA_LAKE_CUSTOM_IMAGE,
    CB_AUTHZ_POWER_USERS,
    CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE,
    DATAHUB_AWS_AUTOSCALING,
    DATAHUB_AZURE_AUTOSCALING,
    DATAHUB_GCP_AUTOSCALING,
    CDP_CCM_V2,
    CDP_CB_DATABASE_WIRE_ENCRYPTION,
    CDP_ENABLE_DISTROX_INSTANCE_TYPES,
    CDP_SHOW_CLI,
    PERSONAL_VIEW_CB_BY_RIGHT,
    CDP_DATA_LAKE_LOAD_BALANCER,
    CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT,
    CDP_USE_DATABUS_CNAME_ENDPOINT,
    CDP_USE_CM_SYNC_COMMAND_POLLER,
    CDP_NETWORK_PREFLIGHT_NOTIFICATIONS,
    CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY,
    FMS_FREEIPA_BATCH_CALL,
    CDP_CB_AZURE_DISK_SSE_WITH_CMK,
    CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION,
    OJDBC_TOKEN_DH,
    CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION;
}
