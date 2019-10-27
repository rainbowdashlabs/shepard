package de.eldoria.shepard.contexts.commands.admin;

import de.eldoria.shepard.contexts.ContextCategory;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.argument.CommandArg;
import de.eldoria.shepard.contexts.commands.argument.SubArg;
import de.eldoria.shepard.database.queries.InviteData;
import de.eldoria.shepard.database.types.DatabaseInvite;
import de.eldoria.shepard.util.TextFormatting;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;

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
import static de.eldoria.shepard.util.Verifier.isArgument;
import static java.lang.System.lineSeparator;

public class Invite extends Command {

    private static final Pattern INVITE = Pattern.compile("([a-zA-Z0-9]{6,7})$");

    /**
     * Creates a new Invite command object.
     */
    public Invite() {
        commandName = "invite";
        commandDesc = DESCRIPTION.replacement;
        commandArgs = new CommandArg[]{
                new CommandArg("action", true,
                        new SubArg("addInvite", C_ADD_INVITE.replacement, true),
                        new SubArg("removeInvite", C_REMOVE_INVITE.replacement, true),
                        new SubArg("refreshInvites", C_REFRESH_INVITES.replacement, true),
                        new SubArg("showInvites", C_SHOW_INVITES.replacement, true)),
                new CommandArg("values", false,
                        new SubArg("addInvite", A_CODE.replacement + " " + A_INVITE_NAME.replacement),
                        new SubArg("removeInvite", A_CODE.replacement),
                        new SubArg("refreshInvite", A_EMPTY.replacement),
                        new SubArg("showInvites", A_EMPTY.replacement))
        };
        category = ContextCategory.ADMIN;
    }


    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        String cmd = args[0];
        if (isArgument(cmd, "addInvite", "ai")) {
            addInvite(args, messageContext);
            return;
        }
        if (isArgument(cmd, "removeInvite", "remi")) {
            removeInvite(args, messageContext);
            return;
        }
        if (isArgument(cmd, "refreshInvites", "refi")) {
            refreshInvites(messageContext);
            return;
        }
        if (isArgument(cmd, "showInvites", "si")) {
            showInvites(messageContext);
            return;
        }
        MessageSender.sendSimpleError(ErrorType.INVALID_ACTION, messageContext);
    }

    private void showInvites(MessageEventDataWrapper messageContext) {
        List<DatabaseInvite> invites = InviteData.getInvites(messageContext.getGuild(), messageContext);

        StringBuilder message = new StringBuilder();
        message.append(M_REGISTERED_INVITES.replacement).append(lineSeparator());

        TextFormatting.TableBuilder tableBuilder = TextFormatting.getTableBuilder(
                invites, M_CODE.replacement, M_USAGE_COUNT.replacement, M_INVITE_NAME.replacement);
        for (DatabaseInvite invite : invites) {
            tableBuilder.next();
            tableBuilder.setRow(invite.getCode(), invite.getUsedCount() + "", invite.getSource());
        }
        message.append(tableBuilder);
        MessageSender.sendMessage(message.toString(), messageContext);
    }

    private void refreshInvites(MessageEventDataWrapper messageContext) {
        messageContext.getGuild().retrieveInvites().queue(invites -> {
            if (InviteData.updateInvite(messageContext.getGuild(), invites, messageContext)) {
                MessageSender.sendMessage(M_REMOVED_NON_EXISTENT_INVITES.replacement, messageContext);
            }
        });
    }

    private void removeInvite(String[] args, MessageEventDataWrapper receivedEvent) {
        if (args.length != 2) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, receivedEvent);
            return;
        }
        List<DatabaseInvite> databaseInvites = InviteData.getInvites(receivedEvent.getGuild(), receivedEvent);

        for (DatabaseInvite invite : databaseInvites) {
            if (invite.getCode().equals(args[1])) {
                if (InviteData.removeInvite(receivedEvent.getGuild(), args[1], receivedEvent)) {
                    MessageSender.sendMessage(M_REMOVED_INVITE.replacement + " **" + invite.getSource()
                            + "**", receivedEvent);
                    return;
                }
            }
        }
        MessageSender.sendSimpleError(ErrorType.NO_INVITE_FOUND,
                receivedEvent);
    }

    private void addInvite(String[] args, MessageEventDataWrapper messageContext) {
        if (args.length < 3) {
            MessageSender.sendSimpleError(ErrorType.TOO_FEW_ARGUMENTS, messageContext);
            return;
        }

        Matcher matcher = INVITE.matcher(args[1]);
        if (!matcher.find()) {
            MessageSender.sendSimpleError(ErrorType.NO_INVITE_FOUND, messageContext);
        }
        String code = matcher.group(1);


        messageContext.getGuild().retrieveInvites().queue(invites -> {
            for (var invite : invites) {
                if (invite.getCode().equals(code)) {
                    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    if (InviteData.addInvite(messageContext.getGuild(), invite.getCode(), name,
                            invite.getUses(), messageContext)) {
                        MessageSender.sendMessage(locale.getReplacedString(M_ADDED_INVITE.localeCode,
                                messageContext.getGuild(),
                                "**" + name + "**",
                                "**" + invite.getCode() + "**",
                                "**" + invite.getUses() + "**"), messageContext);
                    }
                    return;
                }
            }
            MessageSender.sendSimpleError(ErrorType.NO_INVITE_FOUND, messageContext);
        });
    }
}
