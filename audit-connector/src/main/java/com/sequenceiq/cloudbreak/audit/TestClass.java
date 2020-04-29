package com.sequenceiq.cloudbreak.audit;

import com.cloudera.thunderhead.service.audit.AuditGrpc;
import com.cloudera.thunderhead.service.audit.AuditProto;

public class TestClass {

    private io.grpc.MethodDescriptor<AuditProto.ArchiveAuditEventsRequest, AuditProto.ArchiveAuditEventsResponse> stub;

    void doIt() {
        stub = AuditGrpc.getArchiveAuditEventsMethod();

    }
}
