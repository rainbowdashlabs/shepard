package de.eldoria.shepard.contexts.commands.exklusive;

import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.calendar.CalendarEvent;
import de.eldoria.shepard.contexts.commands.Command;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static de.eldoria.shepard.calendar.CalendarQuickstart.getEldoriaMeetings;

public class Meetings extends Command {

    /**
     * Creates a new Meetings command object.
     */
    public Meetings() {
        commandName = "meetings";
        commandAliases = new String[] {"besprechung", "meeting"};
        commandDesc = "Der nächste Besprechungstermin";
    }

    @Override
    public void execute(String label, String[] args, MessageReceivedEvent receivedEvent) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            Date currentDate = cal.getTime();

            List<CalendarEvent> calendarEvent = getEldoriaMeetings();
            SimpleDateFormat getTime = new SimpleDateFormat("HH:mm");
            SimpleDateFormat getDate = new SimpleDateFormat("dd.MM.yyyy");

            ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
            fields.add(new MessageEmbed.Field("Nächste Meetings:", calendarEvent.get(0).getSummary()
                    + " am " + getDate.format(calendarEvent.get(0).getStart()) + " von "
                    + getTime.format(calendarEvent.get(0).getStart()) + " bis "
                    + getTime.format(calendarEvent.get(0).getEnd()) + System.lineSeparator()
                    + calendarEvent.get(1).getSummary() + " am " + getDate.format(calendarEvent.get(1).getStart())
                    + " von " + getTime.format(calendarEvent.get(1).getStart()) + " bis "
                            + getTime.format(calendarEvent.get(1).getEnd()), false));
            MessageSender.sendTextBox(null, fields, receivedEvent.getChannel());
        } catch (IOException | GeneralSecurityException e) {
            receivedEvent.getChannel().sendMessage("**Exception** occurred :confused:").queue();
        }
    }
}
