package com.marksware.simplescheduler.models;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Hashtable;

public class SimpleEvent {

    public static final String  CALENDARID_PRIMARY = "primary";

    private String _CalendarID = null;
    public String GetCalendarID() { return _CalendarID == null ? CALENDARID_PRIMARY : _CalendarID; }
    public void SetCalendarID(String value) { _CalendarID = value; }

    private String _ID = null;
    public String GetID() { return _ID; }

    private String _Summary = "";
    public String GetSummary() { return _Summary != null ? _Summary : ""; }
    public void SetSummary(String value) { _Summary = value; }

    private String _Location = "";
    public String GetLocation() { return _Location != null ? _Location : ""; }
    public void SetLocation(String value) { _Location = value; }

    private String _Description = "";
    public String GetDescription() { return _Description != null ? _Description : ""; }
    public void SetDescription(String value) { _Description = value; }

    private Instant _StartDateTime = Instant.now();
    public Instant GetStartDateTime() { return _StartDateTime; }
    public DateTime GetGoogleStartDateTime() { return InstantToGoogleDateTime(_StartDateTime); }
    public void SetStartDateTime(Instant value) { _StartDateTime = value; }
    public void SetStartDateTime(DateTime value) { _StartDateTime = DateTimeToInstant(value); }
    public void SetStartDateTime(EventDateTime value) { _StartDateTime = EventDateTimeToInstant(value); }
    public void SetStartDateTimeString(String value) { _StartDateTime = StringToInstant(value); } //"2022-02-05T19:00:00.000-08:00"

    private Instant _EndDateTime = Instant.now();
    public Instant GetEndDateTime() { return _EndDateTime; }
    public DateTime GetGoogleEndDateTime() { return InstantToGoogleDateTime(_EndDateTime); }
    public void SetEndDateTime(Instant value) { _EndDateTime = value; }
    public void SetEndDateTime(DateTime value) { _EndDateTime = DateTimeToInstant(value); }
    public void SetEndDateTime(EventDateTime value) { _EndDateTime = EventDateTimeToInstant(value); }
    public void SetEndDateTimeString(String value) { _EndDateTime = StringToInstant(value); } //"2022-02-05T19:00:00.000-08:00"

    private Hashtable<String,String> _ExtendedProperties = new Hashtable<String,String>();
    public Hashtable<String, String> GetExtendedProperties() { return _ExtendedProperties; }

    public static DateTime InstantToGoogleDateTime(Instant instant)
    {
        if (instant != null) {
            var date = DateTime.parseRfc3339(instant.toString());
            return date;
        }
        return null;
    }

    private Instant DateTimeToInstant(DateTime dateTime)
    {
        if (dateTime != null) {
            var date = dateTime.toStringRfc3339();
            var instant = Instant.parse(date);
            return instant;
        }
        return null;
    }

    private Instant EventDateTimeToInstant(EventDateTime eventDateTime)
    {
        if (eventDateTime != null) {
            var dateTime = eventDateTime.getDateTime();
            if (dateTime == null)
                dateTime = eventDateTime.getDate();
            if (dateTime != null) {
                var date = dateTime.toStringRfc3339();
                if (dateTime.isDateOnly())
                    date += "T00:00:00+00:00";
                var instant = Instant.parse(date);
                return instant;
            }
        }
        return null;
    }

    public static Instant StringToInstant(String dateTime)
    {
        if (dateTime != null) {
            var instant = Instant.parse(dateTime);
            return instant;
        }
        return null;
    }

    private DateTime StringToGoogleDateTime(String dateTime)
    {
        if (dateTime != null) {
            var date = DateTime.parseRfc3339(dateTime);
            return date;
        }
        return null;
    }

    public SimpleEvent()
    {
    }

    public SimpleEvent(String calenderID, Event ev)
    {
        SetCalendarID(calenderID);
        _ID = ev.getId();
        SetDescription(ev.getDescription());
        SetSummary(ev.getSummary());
        SetLocation(ev.getLocation());
        SetStartDateTime(ev.getStart());
        SetEndDateTime(ev.getEnd());

        var googleProps = ev.getExtendedProperties();
        if (googleProps != null) {
            var shared = googleProps.getShared();
            if (shared != null) {
                var simpleProps = GetExtendedProperties();
                for (var m : shared.entrySet()) {
                    simpleProps.put(m.getKey(), m.getValue());
                }
            }
        }
    }

    public SimpleEvent(SimpleEvent ev)
    {
        SetCalendarID(ev.GetCalendarID());
        _ID = ev.GetID();
        SetDescription(ev.GetDescription());
        SetSummary(ev.GetSummary());
        SetLocation(ev.GetLocation());
        SetStartDateTime(ev.GetStartDateTime());
        SetEndDateTime(ev.GetEndDateTime());

        var props = GetExtendedProperties();
        var simpleProps = ev.GetExtendedProperties();
        for (var m : simpleProps.entrySet()) {
            props.put(m.getKey(), (String) m.getValue());
        }
    }

    public static Event NewGoogleEvent(SimpleEvent simple)
    {
        var event = new Event();
        event.setSummary(simple.GetSummary());
        event.setLocation(simple.GetLocation());
        event.setDescription(simple.GetDescription());
        EventDateTime appStart = new EventDateTime();
        appStart.setDateTime(simple.GetGoogleStartDateTime());
        event.setStart(appStart);
        EventDateTime appEnd = new EventDateTime();
        appEnd.setDateTime(simple.GetGoogleEndDateTime());
        event.setEnd(appEnd);

        //Recurrence = new String[] { "RRULE:FREQ=WEEKLY;BYDAY=MO" },

        var simpleProps = simple.GetExtendedProperties();
        var appProps = new Event.ExtendedProperties();
        appProps.setShared(simpleProps);
        event.setExtendedProperties(appProps);

        return event;
    }

    public void PrintEvent()
    {
        System.out.printf("[%s]\n", GetSummary());
        //System.out.printf("%s\n", GetLocation());
        //System.out.printf("%s\n", GetDescription());
        var start = LocalDateTime.ofInstant(GetStartDateTime(), ZoneId.systemDefault()).toString().replace("T", " ");
        var end = LocalDateTime.ofInstant(GetEndDateTime(), ZoneId.systemDefault()).toString().replace("T", " ");
        System.out.printf("(%s) To (%s)\n", start.toString(), end.toString());
    }
}
