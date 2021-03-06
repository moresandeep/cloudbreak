package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LoadAlertResponse extends AbstractAlertJson {

    @ApiModelProperty(BaseAlertJsonProperties.CRN)
    private String crn;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyResponse scalingPolicy;

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION)
    private LoadAlertConfigurationResponse loadAlertConfiguration;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public ScalingPolicyResponse getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyResponse scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }

    public LoadAlertConfigurationResponse getLoadAlertConfiguration() {
        return loadAlertConfiguration;
    }

    public void setLoadAlertConfiguration(LoadAlertConfigurationResponse loadAlertConfiguration) {
        this.loadAlertConfiguration = loadAlertConfiguration;
    }
}
