package com.vetsync.sync.application;

import com.vetsync.sync.domain.TimeWindowRecord;
import com.vetsync.sync.domain.TimeWindowScanCommand;

import java.time.LocalDate;
import java.util.List;

public interface TimeWindowSource {

    List<TimeWindowRecord> findEligible(TimeWindowScanCommand command, LocalDate today, long afterSourceId);
}
