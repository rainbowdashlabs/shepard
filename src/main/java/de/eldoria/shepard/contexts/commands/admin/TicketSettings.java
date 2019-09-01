package de.eldoria.shepard.contexts.commands.admin;

import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.CommandArg;
import de.eldoria.shepard.database.queries.TicketData;
import de.eldoria.shepard.database.types.TicketType;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.util.Verifier;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.eldoria.shepard.util.Verifier.getValidRoles;
import static java.lang.System.lineSeparator;

public class TicketSettings extends Command {
    /**
     * Creates a new ticket setting command object.
     */
    public TicketSettings() {
        commandName = "ticketSettings";
        commandDesc = "Manage Ticket settings";
        commandAliases = new String[] {"ts"};
        commandArgs = new CommandArg[] {
                new CommandArg("action",
                        "**__c__reate__T__ype** -> Creates a new ticket type" + lineSeparator()
                                + "**__r__emove__T__ype** -> Removes a ticket type by type name or id"
                                + lineSeparator()
                                + "**__s__et__O__wner__R__oles** -> Sets ticket owner roles for ticket type"
                                + lineSeparator()
                                + "**__s__et__S__upport__R__oles** -> Sets ticket support roles for ticket type"
                                + lineSeparator()
                                + "**__s__et__C__hannel__C__ategory** -> Sets Channel Category for support channel."
                                + lineSeparator()
                                + "**__s__et__C__reation__M__essage** -> Sets the creation Message of the ticket type"
                                + lineSeparator()
                                + "Message will be send when a ticket is created.",
                        true),
                new CommandArg("value",
                        "**createType** -> [type_name] [channel_category_id]" + lineSeparator()
                                + "**removeType** -> [type_name]" + lineSeparator()
                                + "**setOwnerRoles** -> [type_name] [Roles...] One or more Roles" + lineSeparator()
                                + "**setSupportRoles** -> [type_name] [Roles...] One or more Roles" + lineSeparator()
                                + "**setChannelCategory** -> [type_name] [channel_category_id]" + lineSeparator()
                                + "**setCreation Message** -> [type_names] [Message]" + lineSeparator()
                                + "Supported Placeholder: {user_name} {user_tag} {user_mention}",
                        true)};

    }

    @Override
    public void execute(String label, String[] args, MessageReceivedEvent receivedEvent) {
        String cmd = args[0];
        String type = args[1];
        List<TicketType> tickets = TicketData.getTypes(receivedEvent.getGuild(), receivedEvent);
        TicketType scopeTicket = null;
        for (TicketType ticket : tickets) {
            if (ticket.getKeyword().equalsIgnoreCase(type)) {
                scopeTicket = ticket;
                break;
            }
        }

        //All validation operations are inside the method except when they are needed for more than one method.


        if ((cmd.equalsIgnoreCase("createType") || cmd.equalsIgnoreCase("ct")) && scopeTicket != null) {
            createType(args, receivedEvent, type);
            return;
        } else if ((cmd.equalsIgnoreCase("createType") || cmd.equalsIgnoreCase("ct")) && scopeTicket == null) {
            MessageSender.sendSimpleError(ErrorType.TYPE_ALREADY_DEFINED, receivedEvent.getChannel());
            return;
        }

        if (scopeTicket == null) {
            MessageSender.sendSimpleError(ErrorType.TYPE_NOT_FOUND, receivedEvent.getChannel());
            return;
        }

        if (cmd.equalsIgnoreCase("removeType") || cmd.equalsIgnoreCase("rt")) {
            removeType(args, receivedEvent, scopeTicket);
            return;
        }

        if (cmd.equalsIgnoreCase("setOwnerRoles") || cmd.equalsIgnoreCase("sor")
                || cmd.equalsIgnoreCase("setSupportRoles") || cmd.equalsIgnoreCase("ssr")) {
            setRoles(args, receivedEvent, cmd, scopeTicket);
            return;
        }

        if (cmd.equalsIgnoreCase("setChannelCategory") || cmd.equalsIgnoreCase("scc")) {
            setChannelCategory(args, receivedEvent, scopeTicket);
            return;
        }

        if (cmd.equalsIgnoreCase("setCreationMessage") || cmd.equalsIgnoreCase("scm")) {
            setCreationMessage(args, receivedEvent, scopeTicket);
            return;
        }

        MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
        sendCommandUsage(receivedEvent.getChannel());
    }

    private void setCreationMessage(String[] args, MessageReceivedEvent receivedEvent, TicketType scopeTicket) {
        if (args.length < 3) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            sendCommandUsage(receivedEvent.getChannel());
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        TicketData.addType(receivedEvent.getGuild(), null, message,
                scopeTicket.getKeyword(), receivedEvent);
        MessageSender.sendSimpleTextBox("Set creation text for ticket type " + scopeTicket.getKeyword() + " to:",
                message, receivedEvent.getChannel());

    }

    private void setChannelCategory(String[] args, MessageReceivedEvent receivedEvent, TicketType scopeTicket) {
        if (args.length != 3) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            sendCommandUsage(receivedEvent.getChannel());
            return;
        }

        Category category = receivedEvent.getGuild().getCategoryById(args[2]);

        if (category == null) {
            MessageSender.sendSimpleError(ErrorType.INVALID_CATEGORY, receivedEvent.getChannel());
            return;
        }

        TicketData.addType(receivedEvent.getGuild(), category, null,
                scopeTicket.getKeyword(), receivedEvent);
        MessageSender.sendMessage("Set channel category for ticket type \"" + scopeTicket.getKeyword()
                + "\" to " + category.getName(), receivedEvent.getChannel());
    }

    private void setRoles(String[] args, MessageReceivedEvent receivedEvent, String cmd, TicketType scopeTicket) {
        if (args.length < 3) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            sendCommandUsage(receivedEvent.getChannel());
            return;
        }
        String[] roleIds = Arrays.copyOfRange(args, 2, args.length);
        List<Role> validRoles = getValidRoles(receivedEvent.getGuild(), roleIds);

        List<String> roleMentions = new ArrayList<>();
        validRoles.forEach(role -> roleMentions.add(role.getAsMention()));

        if (cmd.equalsIgnoreCase("setOwnerRoles") || cmd.equalsIgnoreCase("sor")) {
            TicketData.setTypeOwnerRoles(receivedEvent.getGuild(), scopeTicket.getKeyword(), validRoles, receivedEvent);

            MessageSender.sendSimpleTextBox("Set the following roles as owner roles for ticket "
                            + scopeTicket.getKeyword(), String.join(lineSeparator() + "", roleMentions),
                    receivedEvent.getChannel());
        } else {
            TicketData.setTypeSupportRoles(receivedEvent.getGuild(), scopeTicket.getKeyword(),
                    validRoles, receivedEvent);

            MessageSender.sendSimpleTextBox("Set the following roles as owner roles for ticket "
                            + scopeTicket.getKeyword(), String.join(lineSeparator() + "", roleMentions),
                    receivedEvent.getChannel());
        }
    }

    private void removeType(String[] args, MessageReceivedEvent receivedEvent, TicketType scopeTicket) {
        if (args.length != 2) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            sendCommandUsage(receivedEvent.getChannel());
            return;
        }

        List<String> channelIdsByType = TicketData.getChannelIdsByType(receivedEvent.getGuild(),
                scopeTicket.getKeyword(), receivedEvent);

        List<TextChannel> validTextChannels = Verifier.getValidTextChannels(receivedEvent.getGuild(),
                channelIdsByType);

        List<String> typeOwnerRoles = TicketData.getTypeOwnerRoles(receivedEvent.getGuild(),
                scopeTicket.getKeyword(), receivedEvent);

        Set<Member> members = new HashSet<>();

        for (TextChannel channel : validTextChannels) {
            String channelOwnerId = TicketData.getChannelOwnerId(receivedEvent.getGuild(), channel, receivedEvent);
            if (channelOwnerId == null) continue;
            Member memberById = receivedEvent.getGuild().getMemberById(channelOwnerId);
            if (memberById == null) continue;
            members.add(memberById);
        }
        for (Member member : members) {
            TicketHelper.removeAndUpdateTicketRoles(receivedEvent, member, typeOwnerRoles);
        }

        TicketData.removeTypeByKeyword(receivedEvent.getGuild(), scopeTicket.getKeyword(), receivedEvent);

        for (TextChannel channel : validTextChannels) {
            channel.delete().queue();
        }

        MessageSender.sendMessage("Removed ticket type **" + scopeTicket.getKeyword()
                        + "** and all channels of this type!",
                receivedEvent.getChannel());
    }

    private void createType(String[] args, MessageReceivedEvent receivedEvent, String type) {
        if (args.length != 3) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent.getChannel());
            sendCommandUsage(receivedEvent.getChannel());
            return;
        }


        Category category = receivedEvent.getGuild().getCategoryById(args[2]);

        if (category == null) {
            MessageSender.sendSimpleError(ErrorType.INVALID_CATEGORY, receivedEvent.getChannel());
            return;
        }

        TicketData.addType(receivedEvent.getGuild(), category, "", type, receivedEvent);

        MessageSender.sendMessage("Created ticket type: **" + type.toLowerCase() + "**", receivedEvent.getChannel());
    }
}
