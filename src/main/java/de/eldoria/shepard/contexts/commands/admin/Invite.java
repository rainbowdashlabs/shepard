package de.eldoria.shepard.contexts.commands.admin;

import de.eldoria.shepard.contexts.ContextCategory;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.argument.CommandArgument;
import de.eldoria.shepard.contexts.commands.argument.SubArgument;
import de.eldoria.shepard.database.queries.InviteData;
import de.eldoria.shepard.database.types.DatabaseInvite;
import de.eldoria.shepard.localization.util.TextLocalizer;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.util.TextFormatting;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_EMPTY;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.A_CODE;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.A_INVITE_NAME;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.C_ADD_INVITE;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.C_REFRESH_INVITES;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.C_REMOVE_INVITE;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.C_SHOW_INVITES;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.DESCRIPTION;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_ADDED_INVITE;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_CODE;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_INVITE_NAME;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_REGISTERED_INVITES;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_REMOVED_INVITE;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_REMOVED_NON_EXISTENT_INVITES;
import static de.eldoria.shepard.localization.enums.commands.admin.InviteLocale.M_USAGE_COUNT;
import static de.eldoria.shepard.localization.util.TextLocalizer.localizeAllAndReplace;
import static java.lang.System.lineSeparator;

public class Invite extends Command {

    private static final Pattern INVITE = Pattern.compile("([a-zA-Z0-9]{6,7})$");

    /**
     * Creates a new Invite command object.
     */
    public Invite() {
        commandName = "invite";
        commandDesc = DESCRIPTION.tag;
        commandArguments = new CommandArgument[] {
                new CommandArgument("action", true,
                        new SubArgument("add", C_ADD_INVITE.tag, true),
                        new SubArgument("remove", C_REMOVE_INVITE.tag, true),
                        new SubArgument("refresh", C_REFRESH_INVITES.tag, true),
                        new SubArgument("list", C_SHOW_INVITES.tag, true)),
                new CommandArgument("values", false,
                        new SubArgument("add", A_CODE.tag + " " + A_INVITE_NAME.tag),
                        new SubArgument("remove", A_CODE.tag),
                        new SubArgument("refresh", A_EMPTY.tag),
                        new SubArgument("list", A_EMPTY.tag))
        };
        category = ContextCategory.ADMIN;
    }


    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        String cmd = args[0];
        CommandArgument arg = commandArguments[0];
        if (arg.isSubCommand(cmd, 0)) {
            addInvite(args, messageContext);
            return;
        }
        if (arg.isSubCommand(cmd, 1)) {
            removeInvite(args, messageContext);
            return;
        }
        if (arg.isSubCommand(cmd, 2)) {
            refreshInvites(messageContext);
            return;
        }
        if (arg.isSubCommand(cmd, 3)) {
            listInvites(messageContext);
            return;
        }
        MessageSender.sendSimpleError(ErrorType.INVALID_ACTION, messageContext.getTextChannel());
    }

    private void listInvites(MessageEventDataWrapper messageContext) {
        List<DatabaseInvite> invites = InviteData.getInvites(messageContext.getGuild(), messageContext);

        StringBuilder message = new StringBuilder();
        message.append(M_REGISTERED_INVITES.tag).append(lineSeparator());

        TextFormatting.TableBuilder tableBuilder = TextFormatting.getTableBuilder(
                invites, TextLocalizer.localizeAll(M_CODE.tag, messageContext.getGuild()),
                TextLocalizer.localizeAll(M_USAGE_COUNT.tag, messageContext.getGuild()),
                TextLocalizer.localizeAll(M_INVITE_NAME.tag, messageContext.getGuild()));
        for (DatabaseInvite invite : invites) {
            tableBuilder.next();
            tableBuilder.setRow(invite.getCode(), invite.getUsedCount() + "", invite.getSource());
        }
        message.append(tableBuilder);
        MessageSender.sendMessage(message.toString(), messageContext.getTextChannel());
    }

    private void refreshInvites(MessageEventDataWrapper messageContext) {
        messageContext.getGuild().retrieveInvites().queue(invites -> {
            if (InviteData.updateInvite(messageContext.getGuild(), invites, messageContext)) {
                MessageSender.sendMessage(M_REMOVED_NON_EXISTENT_INVITES.tag, messageContext.getTextChannel());
            }
        });
    }

    private void removeInvite(String[] args, MessageEventDataWrapper messageContext) {
        if (args.length != 2) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, messageContext.getTextChannel());
            return;
        }
        List<DatabaseInvite> databaseInvites = InviteData.getInvites(messageContext.getGuild(), messageContext);

        for (DatabaseInvite invite : databaseInvites) {
            if (invite.getCode().equals(args[1])) {
                if (InviteData.removeInvite(messageContext.getGuild(), args[1], messageContext)) {
                    MessageSender.sendMessage(M_REMOVED_INVITE.tag + " **" + invite.getSource()
                            + "**", messageContext.getTextChannel());
                    return;
                }
            }
        }
        MessageSender.sendSimpleError(ErrorType.NO_INVITE_FOUND, messageContext.getTextChannel());
    }

    private void addInvite(String[] args, MessageEventDataWrapper messageContext) {
        if (args.length < 3) {
            MessageSender.sendSimpleError(ErrorType.TOO_FEW_ARGUMENTS, messageContext.getTextChannel());
            return;
        }

        Matcher matcher = INVITE.matcher(args[1]);
        if (!matcher.find()) {
            MessageSender.sendSimpleError(ErrorType.NO_INVITE_FOUND, messageContext.getTextChannel());
        }
        String code = matcher.group(1);


        messageContext.getGuild().retrieveInvites().queue(invites -> {
            for (var invite : invites) {
                if (invite.getCode().equals(code)) {
                    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    if (InviteData.addInvite(messageContext.getGuild(), invite.getCode(), name,
                            invite.getUses(), messageContext)) {
                        MessageSender.sendMessage(localizeAllAndReplace(M_ADDED_INVITE.tag,
                                messageContext.getGuild(),
                                "**" + name + "**",
                                "**" + invite.getCode() + "**",
                                "**" + invite.getUses() + "**"), messageContext.getTextChannel());
                    }
                    return;
                }
            }
            MessageSender.sendSimpleError(ErrorType.NO_INVITE_FOUND, messageContext.getTextChannel());
        });
    }
}
