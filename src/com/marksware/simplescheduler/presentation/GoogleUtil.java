package com.marksware.simplescheduler.presentation;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.marksware.simplescheduler.models.*;

public class GoogleUtil {
    private static NetHttpTransport _HTTP_TRANSPORT = null;
    private static Credential _Credentials = null;
    private static Calendar _CalendarService = null;

    private static final String APPLICATION_NAME = "SimpleScheduler";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private static NetHttpTransport GetTransport() throws IOException, GeneralSecurityException
    {
        if (_HTTP_TRANSPORT == null) {
            _HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        }
        return _HTTP_TRANSPORT;
    }

    private static Credential GetCredentials() throws IOException, GeneralSecurityException {
        if (_Credentials == null) {
            var transport = GetTransport();
            File file = new File(CREDENTIALS_FILE_PATH);
            //System.out.printf("Full path to credentials is %s\n", file.getAbsolutePath());

            InputStream in = GoogleUtil.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    transport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            _Credentials = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
        return _Credentials;
    }

    private static Calendar GetCalendar() throws IOException, GeneralSecurityException
    {
        if (_CalendarService == null) {
            _CalendarService = new Calendar.Builder(GetTransport(), JSON_FACTORY, GetCredentials())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return _CalendarService;
    }

    public static List<SimpleEvent> GetEvents(String calendarID, int maxEvents) throws IOException, GeneralSecurityException {

        var simples = new ArrayList<SimpleEvent>();

        try {
            Instant instant = Instant.now();
            instant = instant.minus(365, ChronoUnit.DAYS);
            DateTime minTime = SimpleEvent.InstantToGoogleDateTime(instant);
            var calID = calendarID == null ? SimpleEvent.CALENDARID_PRIMARY : calendarID;
            Events events = GetCalendar().events().list(calID)
                    .setMaxResults(maxEvents)
                    .setTimeMin(minTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            for (var i = items.iterator(); i.hasNext();) {
                Event item = i.next();
                simples.add(new SimpleEvent(calID, item));
            }
        } catch (Exception ex) {
            System.out.printf("Exception: %s", ex.getMessage());
        }
        return simples;
    }

    public static void AddEvent(SimpleEvent simple) throws IOException, GeneralSecurityException {

        try {
            var event = SimpleEvent.NewGoogleEvent(simple);
            var ev = GetCalendar().events().insert(simple.GetCalendarID(), event);
            ev.execute();
        } catch (Exception ex) {
            System.out.printf("Exception: %s", ex.getMessage());
        }
    }

    public static void UpdateEvent(SimpleEvent simple) throws IOException, GeneralSecurityException {

        try {
            var event = SimpleEvent.NewGoogleEvent(simple);
            var ev = GetCalendar().events().update(simple.GetCalendarID(), simple.GetID(), event);
            ev.execute();
        } catch (Exception ex) {
            System.out.printf("Exception: %s", ex.getMessage());
        }
    }

    public static void DeleteEvent(SimpleEvent simple) throws IOException, GeneralSecurityException {

        try {
            var ev = GetCalendar().events().delete(simple.GetCalendarID(), simple.GetID());
            ev.execute();
        } catch (Exception ex) {
            System.out.printf("Exception: %s", ex.getMessage());
        }
    }
}