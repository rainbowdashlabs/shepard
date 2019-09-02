package de.eldoria.shepard.contexts.commands.admin;

import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.CommandArg;
import de.eldoria.shepard.database.queries.ChangelogData;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.eldoria.shepard.database.DbUtil.getIdRaw;
import static java.lang.System.lineSeparator;

public class Changelog extends Command {

    /**
     * Creates a new changelog command object.
     */
    public Changelog() {
        commandName = "changelog";
        commandAliases = new String[] {"log"};
        commandDesc = "provides function to log role changes on a guild";
        commandArgs = new CommandArg[] {
                new CommandArg("action",
                        "**__a__dd__R__ole** -> Adds a role to changelog" + lineSeparator()
                                + "**__r__emove__R__ole** -> Removes a role from changelog" + lineSeparator()
                                + "**__a__ctivate** -> Activates changelog posting in a channel."
                                + "Overrides old channel if set." + lineSeparator()
                                + "**__d__eactivate** -> Deactivates changelog postings" + lineSeparator()
                                + "**__r__oles** -> Shows currently observed roles", true),
                new CommandArg("value",
                        "**addRole** -> [role]" + lineSeparator()
                                + "**removeRole** -> [role]" + lineSeparator()
                                + "**activate** -> [channel]" + lineSeparator()
                                + "**deactivate** -> leave empty" + lineSeparator()
                                + "**roles** -> leave empty", false)};
    }

    @Override
    public void execute(String label, String[] args, MessageReceivedEvent receivedEvent) {
        String cmd = args[0];
        if (cmd.equalsIgnoreCase("addRole") || cmd.equalsIgnoreCase("ar")
                || cmd.equalsIgnoreCase("removeRole") || cmd.equalsIgnoreCase("rr")) {
            modifyRoles(args, receivedEvent, cmd);
            return;
        }

        if (cmd.equalsIgnoreCase("activate") || cmd.equalsIgnoreCase("a")) {
            activate(args, receivedEvent);
            return;
        }

        if (cmd.equalsIgnoreCase("deactivate") || cmd.equalsIgnoreCase("d")) {
            deactivate(receivedEvent);
            return;
        }

        if (cmd.equalsIgnoreCase("roles") || cmd.equalsIgnoreCase("r")) {
            showRoles(receivedEvent);
            return;
        }

        MessageSender.sendSimpleError(ErrorType.INVALID_ACTION, receivedEvent.getChannel());
        sendCommandUsage(receivedEvent.getChannel());
    }

    private void showRoles(MessageReceivedEvent receivedEvent) {
        List<String> roleIds = ChangelogData.getRoles(receivedEvent.getGuild(), receivedEvent);

        List<String> roleMentions = roleIds.stream()
                .map(roleId -> receivedEvent.getGuild().getRoleById(getIdRaw(roleId)))
                .filter(Objects::nonNull).map(IMentionable::getAsMention)
                .collect(Collectors.toList());

        MessageSender.sendSimpleTextBox("Currently logged roles:",
                String.join(lineSeparator(), roleMentions), receivedEvent.getChannel());
    }

    private void deactivate(MessageReceivedEvent receivedEvent) {
        ChangelogData.removeChannel(receivedEvent.getGuild(), receivedEvent);

        MessageSender.sendMessage("Changelog is deactivated", receivedEvent.getChannel());
    }

    private void activate(String[] args, MessageReceivedEvent receivedEvent) {
        if (args.length != 2) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            return;
        }

        TextChannel textChannelById = receivedEvent.getGuild().getTextChannelById(getIdRaw(args[1]));

        if (textChannelById == null) {
            MessageSender.sendSimpleError(ErrorType.INVALID_CHANNEL, receivedEvent.getChannel());
            return;
        }

        ChangelogData.setChannel(receivedEvent.getGuild(), textChannelById, receivedEvent);

        MessageSender.sendMessage("Changelog is presented in channel" + textChannelById.getAsMention(),
                receivedEvent.getChannel());
    }

    private void modifyRoles(String[] args, MessageReceivedEvent receivedEvent, String cmd) {
        if (args.length != 2) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            return;
        }

        Role roleById = receivedEvent.getGuild().getRoleById(getIdRaw(args[1]));
        if (roleById == null) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ROLE, receivedEvent.getChannel());
            return;
        }

        if (cmd.equalsIgnoreCase("addRole") || cmd.equalsIgnoreCase("ar")) {
            ChangelogData.addRole(receivedEvent.getGuild(), roleById, receivedEvent);
            MessageSender.sendMessage("Added role **" + roleById.getName() + "** to changelog.",
                    receivedEvent.getChannel());
        } else {
            ChangelogData.removeRole(receivedEvent.getGuild(), roleById, receivedEvent);
            MessageSender.sendMessage("Removed role **" + roleById.getName() + "** from changelog.",
                    receivedEvent.getChannel());
        }
    }
}