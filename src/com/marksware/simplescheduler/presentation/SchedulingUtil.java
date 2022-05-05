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


    private static void ApplySchedulingCommands(String calendarID, List<SimpleEvent> inEvents,
                                                List<SimpleEvent> outAddEvents, List<SimpleEvent> outDeleteEvents, List<SimpleEvent> outUpdateEvents) {
        // Get list of events to add
        for (var e : inEvents) {

            // Command processing
            // Split an event into two equal parts and add the second part to the next day
            if (e.GetDescription().contains(SchedulingCommands._SimpleSchedule_SplitInHalfToNextDay.toString())) {
                System.out.printf("Processing command (%s) for appointment...\n", SchedulingCommands._SimpleSchedule_SplitInHalfToNextDay.toString());
                e.PrintEvent();

                // Half the time of the current event and add a copy to the next day
                var addEvent = new SimpleEvent(e);
                var instant = addEvent.GetStartDateTime();
                instant = instant.plus(1, ChronoUnit.DAYS);
                var start = addEvent.GetStartDateTime();
                var end = addEvent.GetEndDateTime();
                var diff = Duration.between(start, end);
                var seconds = diff.toSeconds() / 2;
                addEvent.SetStartDateTime(instant);
                addEvent.SetEndDateTime(instant.plus(seconds, ChronoUnit.SECONDS));
                outAddEvents.add(addEvent);

                // Update the current event to be half the time
                var updateEvent = new SimpleEvent(e);
                start = updateEvent.GetStartDateTime();
                end = updateEvent.GetEndDateTime();
                diff = Duration.between(start, end);
                seconds = diff.toSeconds() / 2;
                updateEvent.SetEndDateTime(start.plus(seconds, ChronoUnit.SECONDS));
                outUpdateEvents.add(updateEvent);
            }
        }
    }

    /*
    Expand one or more events into the those that need to be added, deleted and updated
     */
    public static void ExpandEvents(String calendarID, List<SimpleEvent> inEvents,
                                    List<SimpleEvent> outAddEvents, List<SimpleEvent> outDeleteEvents, List<SimpleEvent> outUpdateEvents)
            throws IOException, GeneralSecurityException {

        // Restore calendar to original state
        RestoreCalendar(calendarID, inEvents, outAddEvents, outDeleteEvents, outUpdateEvents);

        // Apply any scheduling commands found
        ApplySchedulingCommands(calendarID, inEvents, outAddEvents, outDeleteEvents, outUpdateEvents);
    }

    private static void RestoreCalendar(String calendarID, List<SimpleEvent> inEvents,
            List<SimpleEvent> outAddEvents, List<SimpleEvent> outDeleteEvents, List<SimpleEvent> outUpdateEvents) {
        // Remove all events that we've generated
        for (var e : inEvents) {
            for (var p : e.GetExtendedProperties().entrySet()) {
                if (p.getKey().contains(ScheduledIdentifiers._SimpleSchedule_Generated.toString())) {
                    outDeleteEvents.add(e);
                    break;
                }
            }
        }
        for (var e : outDeleteEvents) {
            inEvents.remove(e);
        }

        // Restore all non-generated SimpleSchedule appointments to their original state
        for (var e : inEvents) {
            boolean hasCommand = e.GetDescription().contains(SchedulingCommands._SimpleSchedule_.toString());

            if (hasCommand) {
                var ssOriginalStart = e.GetStartDateTime();
                var ssOriginalEnd = e.GetEndDateTime();
                var ssUpdatedStart = e.GetStartDateTime();
                var ssUpdatedEnd = e.GetEndDateTime();

                for (var p : e.GetExtendedProperties().entrySet()) {
                    var key = p.getKey();
                    if (key.contains(ScheduledIdentifiers._SimpleSchedule_OriginalStartDateTime.toString())) {
                        ssOriginalStart = SimpleEvent.StringToInstant(p.getValue().toString());
                    } else if (key.contains(ScheduledIdentifiers._SimpleSchedule_OriginalEndDateTime.toString())) {
                        ssOriginalEnd = SimpleEvent.StringToInstant(p.getValue().toString());
                    } else if (key.contains(ScheduledIdentifiers._SimpleSchedule_UpdatedStartDateTime.toString())) {
                        ssUpdatedStart = SimpleEvent.StringToInstant(p.getValue().toString());
                    } else if (key.contains(ScheduledIdentifiers._SimpleSchedule_UpdatedEndDateTime.toString())) {
                        ssUpdatedEnd = SimpleEvent.StringToInstant(p.getValue().toString());
                    }
                }

                var start = e.GetStartDateTime();
                var end = e.GetEndDateTime();
                if (start.equals(ssUpdatedStart) && end.equals(ssUpdatedEnd)) {
                    start = ssOriginalStart;
                    end = ssOriginalEnd;
                } else {
                    ssOriginalStart = start;
                    ssUpdatedStart = start;
                    ssOriginalEnd = end;
                    ssUpdatedEnd = end;
                }

                e.SetStartDateTime(start);
                e.SetEndDateTime(end);

                var props = e.GetExtendedProperties();
                props.put(ScheduledIdentifiers._SimpleSchedule_OriginalStartDateTime.toString(),
                        SimpleEvent.InstantToGoogleDateTime(ssOriginalStart).toStringRfc3339());
                props.put(ScheduledIdentifiers._SimpleSchedule_OriginalEndDateTime.toString(),
                        SimpleEvent.InstantToGoogleDateTime(ssOriginalEnd).toStringRfc3339());
                props.put(ScheduledIdentifiers._SimpleSchedule_UpdatedStartDateTime.toString(),
                        SimpleEvent.InstantToGoogleDateTime(ssUpdatedStart).toStringRfc3339());
                props.put(ScheduledIdentifiers._SimpleSchedule_UpdatedEndDateTime.toString(),
                        SimpleEvent.InstantToGoogleDateTime(ssUpdatedEnd).toStringRfc3339());
                var newEvent = new SimpleEvent(e);
                outUpdateEvents.add(newEvent);
            }
        }
    }

    public static void DeleteEvents(String calendarID, List<SimpleEvent> deleteEvents)
            throws IOException, GeneralSecurityException {

        System.out.printf("\n--- Deleting ---\n");
        for (var e : deleteEvents) {
            e.PrintEvent();
            GoogleUtil.DeleteEvent(e);
        }
    }

    public static void UpdateEvents(String calendarID, List<SimpleEvent> updateEvents)
            throws IOException, GeneralSecurityException {

        System.out.printf("\n--- Updating ---\n");
        for (var e : updateEvents) {

            // Store the updated start and end times so we can tell if the user has manually modified them
            var props = e.GetExtendedProperties();
            props.put(ScheduledIdentifiers._SimpleSchedule_UpdatedStartDateTime.toString(), e.GetGoogleStartDateTime().toStringRfc3339());
            props.put(ScheduledIdentifiers._SimpleSchedule_UpdatedEndDateTime.toString(), e.GetGoogleEndDateTime().toStringRfc3339());
            e.PrintEvent();
            GoogleUtil.UpdateEvent(e);
        }
    }

    public static void AddEvents(String calendarID, List<SimpleEvent> addEvents)
            throws IOException, GeneralSecurityException {
        System.out.printf("\n--- Adding ---\n");
        for (var e : addEvents) {

            // Mark all added events as generated
            var props = e.GetExtendedProperties();
            props.put(ScheduledIdentifiers._SimpleSchedule_Generated.toString(), "");
            e.PrintEvent();
            GoogleUtil.AddEvent(e);
            System.out.printf("\n");
        }
    }

    public static void ProcessEvents(String calendarID, List<SimpleEvent> inEvents)
            throws IOException, GeneralSecurityException {
            var outAddEvents = new ArrayList<SimpleEvent>();
            var outDeleteEvents = new ArrayList<SimpleEvent>();
            var outUpdateEvents = new ArrayList<SimpleEvent>();

            ExpandEvents(calendarID, inEvents, outAddEvents, outDeleteEvents, outUpdateEvents);
            DeleteEvents(calendarID, outDeleteEvents);
            UpdateEvents(calendarID, outUpdateEvents);
            AddEvents(calendarID, outAddEvents);
    }

}