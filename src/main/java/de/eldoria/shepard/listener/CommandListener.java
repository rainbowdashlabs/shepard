package de.eldoria.shepard.listener;

import de.eldoria.shepard.collections.CommandCollection;
import de.eldoria.shepard.database.queries.PrefixData;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.exceptions.CommandException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Arrays;


public class CommandListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel() instanceof PrivateChannel) {
            if (event.getAuthor().isBot()) return;
            event.getChannel().sendMessage("I'm too shy. Please speak to me on a public Server.").queue();
            return;
        }
        Message message = event.getMessage();
        String receivedMessage = message.getContentRaw();
        String[] args = receivedMessage.split(" ");
        args[0] = args[0].replace(PrefixData.getPrefix(event.getGuild(), event), "");

        if (checkPrefix(receivedMessage, event.getGuild(), event)) {
            //BotCheck
            if (event.getAuthor().isBot()) {
                MessageSender.sendMessage("I'm not allowed to talk to you " + event.getAuthor().getName()
                        + ". Please leave me alone ._.", event.getChannel());
                return;
            }

            //Command execution
            Command command = CommandCollection.getInstance().getCommand(args[0]);
            if (command != null && command.isContextValid(event)) {
                //TODO Check Arg length of command
                String label = args[0];

                if (args.length > 1) {
                    args = Arrays.copyOfRange(args, 1, args.length);
                } else {
                    args = new String[0];
                }
                if (command.checkArguments(args)) {
                    try {
                        command.execute(label, args, event);
                    } catch (CommandException e) {
                        MessageSender.sendSimpleError(e.getMessage(), event.getChannel());
                    }
                } else {
                    MessageSender.sendSimpleError(ErrorType.TOO_FEW_ARGUMENTS, event.getChannel());
                    command.sendCommandUsage(event.getChannel());
                }
                return;
            }

            MessageSender.sendError(new MessageEmbed.Field[] {new MessageEmbed.Field("Command not found!", "Type "
                    + PrefixData.getPrefix(event.getGuild(), event)
                    + "help for a full list of available commands!", false)}, event.getChannel());
        }
    }

    private boolean checkPrefix(String message, Guild guild, MessageReceivedEvent event) {
        return message.startsWith(PrefixData.getPrefix(event.getGuild(), event));
    }
}

