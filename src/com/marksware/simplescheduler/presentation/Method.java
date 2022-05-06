
package com.marksware.simplescheduler.presentation;

import com.marksware.simplescheduler.models.SimpleEvent;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class SchedulingUtil {

    public enum ScheduledIdentifiers { _SimpleSchedule_,
        _SimpleSchedule_Generated,
        _SimpleSchedule_OriginalStartDateTime, _SimpleSchedule_OriginalEndDateTime,
        _SimpleSchedule_UpdatedStartDateTime, _SimpleSchedule_UpdatedEndDateTime
    };

    public enum SchedulingCommands { _SimpleSchedule_,
        _SimpleSchedule_SplitInHalfToNextDay
    };
