package com.vetsync.sync.adapter.in.scheduler;

import com.vetsync.sync.application.PollMasterDataUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sync.poll.enabled", havingValue = "true")
public class MasterDataPollScheduler {
    private final PollMasterDataUseCase pollMasterDataUseCase;

    public MasterDataPollScheduler(PollMasterDataUseCase pollMasterDataUseCase) {
        this.pollMasterDataUseCase = pollMasterDataUseCase;
    }

    @Scheduled(fixedDelayString = "${sync.poll.delay-ms:5000}")
    public void poll() {
        pollMasterDataUseCase.poll();
    }
}
