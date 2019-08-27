package com.sequenceiq.environment.parameters.dao.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AZURE")
public class AzureParameters extends BaseParameters {

}
