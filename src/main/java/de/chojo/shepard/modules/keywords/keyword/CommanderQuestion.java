package de.chojo.shepard.modules.keywords.keyword;

import de.chojo.shepard.messageHandler.Messages;
import de.chojo.shepard.modules.keywords.Keyword;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommanderQuestion extends Keyword {
    public CommanderQuestion() {
        keywords = new String[]{"commander?", "shepard?"};
    }

    @Override
    public boolean execute(MessageReceivedEvent event, String key) {
        if(key.equalsIgnoreCase(keywords[0])){
            Messages.sendMessage("Commander Shepard meldet sich zum Dienst!", event.getChannel());
        }
        if(key.equalsIgnoreCase(keywords[1])){
            Messages.sendMessage("Hier bin ich o/", event.getChannel());
        }
        return true;
    }
}
