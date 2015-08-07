package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;

@Component
public class GetSSHFingerprintsHandler implements CloudPlatformEventHandler<GetSSHFingerprintsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetSSHFingerprintsHandler.class);
    private static final int INTERVAL = 5;
    private static final int MAX_ATTEMPT = 100;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<InstanceConsoleOutputResult> syncPollingScheduler;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetSSHFingerprintsRequest> type() {
        return GetSSHFingerprintsRequest.class;
    }

    @Override
    public void accept(Event<GetSSHFingerprintsRequest> getSSHFingerprintsRequestEvent) {
        LOGGER.info("Received event: {}", getSSHFingerprintsRequestEvent);
        GetSSHFingerprintsRequest fingerprintsRequest = getSSHFingerprintsRequestEvent.getData();
        try {
            CloudContext cloudContext = fingerprintsRequest.getCloudContext();
            String platform = cloudContext.getPlatform();
            CloudInstance cloudInstance = fingerprintsRequest.getCloudInstance();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authenticate(cloudContext, fingerprintsRequest.getCloudCredential());
            String initialConsoleOutput = connector.instances().getConsoleOutput(ac, cloudInstance);
            InstanceConsoleOutputResult consoleOutputResult = new InstanceConsoleOutputResult(cloudContext, cloudInstance, initialConsoleOutput);
            PollTask<InstanceConsoleOutputResult> outputPollerTask = statusCheckFactory.newPollInstanceConsoleOutputTask(ac, cloudInstance);
            if (!outputPollerTask.completed(consoleOutputResult)) {
                consoleOutputResult = syncPollingScheduler.schedule(outputPollerTask, INTERVAL, MAX_ATTEMPT);
            }
            Set<String> sshFingerprints = FingerprintParserUtil.parseFingerprints(consoleOutputResult.getConsoleOutput());
            GetSSHFingerprintsResult fingerprintsResult;
            if (sshFingerprints.isEmpty()) {
                fingerprintsResult = new GetSSHFingerprintsResult("Failed to get SSH fingerprints from the specified VM instance.", null, fingerprintsRequest);
            } else {
                fingerprintsResult = new GetSSHFingerprintsResult(fingerprintsRequest, sshFingerprints);
            }
            fingerprintsRequest.getResult().onNext(fingerprintsResult);
        } catch (Exception e) {
            LOGGER.error("Failed to handle GetSSHFingerprintsRequest: {}", e);
            fingerprintsRequest.getResult().onNext(new GetSSHFingerprintsResult("Failed to get ssh fingerprints!", e, fingerprintsRequest));
        }
        LOGGER.info("GetSSHFingerprintsHandler finished");
    }

    private static class FingerprintParserUtil {

        private static final Logger LOGGER = LoggerFactory.getLogger(FingerprintParserUtil.class);

        private static final Pattern[] FINGERPRINT_PATTERNS = {
                Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*ECDSA"),
                Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*RSA")
        };

        private FingerprintParserUtil() {
        }

        public static Set<String> parseFingerprints(String consoleLog) {
            LOGGER.debug("Received console log: {}", consoleLog);
            Set<String> matchedFingerprints = new HashSet<>();
            String[] lines = consoleLog.split("\n");
            for (String line : lines) {
                for (Pattern pattern : FINGERPRINT_PATTERNS) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        matchedFingerprints.add(m.group("fingerprint"));
                    }
                }
            }
            return matchedFingerprints;
        }
    }
}
