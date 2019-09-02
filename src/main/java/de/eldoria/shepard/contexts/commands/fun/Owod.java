package de.eldoria.shepard.contexts.commands.fun;

import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.contexts.commands.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Owod extends Command {

    /**
     * Creates a new owod keyword.
     */
    public Owod() {
        commandName = "owod";
        commandDesc = "OWO and delete";
    }


    @Override
    public void execute(String label, String[] args, MessageReceivedEvent receivedEvent) {
        MessageSender.sendMessage(":regional_indicator_o::regional_indicator_w::regional_indicator_o:",
                receivedEvent.getChannel());
        MessageSender.deleteMessage(receivedEvent);
    }
}