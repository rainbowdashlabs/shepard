package de.chojo.shepard.contexts.keywords.keyword;

import de.chojo.shepard.messagehandler.MessageSender;
import de.chojo.shepard.contexts.keywords.Keyword;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kodehawa.lib.imageboards.DefaultImageBoards;
import net.kodehawa.lib.imageboards.entities.BoardImage;

import java.util.List;
import java.util.Random;

public class Nudes extends Keyword {

    /**
     * creates a new nudes keyword object.
     */
    public Nudes() {
        keywords = new String[]{"nudes", "pr0n", "porn", "p0rn", "noot"};
    }

    @Override
    public boolean execute(MessageReceivedEvent event, String key) {
        if (event.getChannel().getType() == ChannelType.TEXT) {
            TextChannel channel = (TextChannel) event.getChannel();
            if (channel.isNSFW()) {
                DefaultImageBoards.KONACHAN.search(40, "femshep").async(rule34Images -> {
                    Random rand = new Random();
                    BoardImage image;
                    boolean found;
                    do {
                        image = rule34Images.get(rand.nextInt(rule34Images.size()));
                        List<String> tags = image.getTags();
                        found = false;
                        for (String tag : tags) {
                            if (tag.equalsIgnoreCase("dickgirl")
                                    || tag.equalsIgnoreCase("futanari")
                                    || tag.equalsIgnoreCase("horse")
                                    || tag.equalsIgnoreCase("horse penis")
                                    || tag.equalsIgnoreCase("shemale")
                                    || tag.equalsIgnoreCase("zoophilia")) {
                                found = true;
                                break;
                            }
                        }
                        //TODO: Handle if every picture has one of these tags
                    } while (found);
                    MessageSender.sendMessage(image.getURL(), event.getChannel());
                });
            }
        }
        MessageSender.sendMessage("Someone said " + key + "?", event.getChannel());


        return false;
    }

}