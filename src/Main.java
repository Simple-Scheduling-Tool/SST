import com.marksware.simplescheduler.models.SimpleEvent;
import com.marksware.simplescheduler.presentation.GoogleUtil;
import com.marksware.simplescheduler.presentation.SchedulingUtil;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class Main {
    public static void main(final String[] args) throws Exception {
        System.out.printf("************************\n");
        System.out.printf("*** Simple Scheduler ***\n");
        System.out.printf("************************\n\n");
        try {
            var events = GoogleUtil.GetEvents(null, 100);

            SchedulingUtil.ProcessEvents(SimpleEvent.CALENDARID_PRIMARY, events);
        } catch (Exception ex) {
            System.out.printf(ex.getMessage());
        }
    }
}
