package de.eldoria.shepard.commandmodules.commandsettings.commands;

import de.eldoria.shepard.basemodules.commanddispatching.util.ArgumentParser;
import de.eldoria.shepard.commandmodules.Command;
import de.eldoria.shepard.commandmodules.argument.Parameter;
import de.eldoria.shepard.commandmodules.argument.SubCommand;
import de.eldoria.shepard.commandmodules.command.Executable;
import de.eldoria.shepard.commandmodules.command.GuildChannelOnly;
import de.eldoria.shepard.commandmodules.commandsettings.data.CommandData;
import de.eldoria.shepard.commandmodules.util.CommandCategory;
import de.eldoria.shepard.localization.util.TextLocalizer;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.modulebuilder.requirements.ReqDataSource;
import de.eldoria.shepard.modulebuilder.requirements.ReqParser;
import de.eldoria.shepard.util.BooleanState;
import de.eldoria.shepard.wrapper.EventWrapper;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.OptionalInt;

import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.AD_COMMAND_NAME;
import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.AD_SECONDS;
import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_BOOLEAN;
import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_COMMAND_NAME;
import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_SECONDS;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.C_ADMIN;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.C_GUILD_COOLDOWN;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.C_NSFW;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.C_USER_COOLDOWN;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.DESCRIPTION;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.M_ACTIVATED_ADMIN;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.M_ACTIVATED_NSFW;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.M_DEACTIVATED_ADMIN;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.M_DEACTIVATED_NSFW;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.M_SET_GUILD_COOLDOWN;
import static de.eldoria.shepard.localization.enums.commands.botconfig.ManageContextLocale.M_SET_USER_COOLDOWN;

/**
 * Manage the basic settings of a registered and active {@link Command}.
 */
public class ManageCommand extends Command implements GuildChannelOnly, Executable, ReqParser, ReqDataSource {

    private ArgumentParser parser;
    private CommandData commandData;

    /**
     * Creates a new manage context command object.
     */
    public ManageCommand() {
        super("manageCommand",
                new String[] {"mc"},
                DESCRIPTION.tag,
                SubCommand.builder("manageCommand")
                        .addSubcommand(C_NSFW.tag,
                                Parameter.createCommand("setNsfw"),
                                Parameter.createInput(A_COMMAND_NAME.tag, AD_COMMAND_NAME.tag, true),
                                Parameter.createInput(A_BOOLEAN.tag, null, true))
                        .addSubcommand(C_ADMIN.tag,
                                Parameter.createCommand("setAdminOnly"),
                                Parameter.createInput(A_COMMAND_NAME.tag, AD_COMMAND_NAME.tag, true),
                                Parameter.createInput(A_BOOLEAN.tag, null, true))
                        .addSubcommand(C_USER_COOLDOWN.tag,
                                Parameter.createCommand("setUserCooldown"),
                                Parameter.createInput(A_COMMAND_NAME.tag, AD_COMMAND_NAME.tag, true),
                                Parameter.createInput(A_SECONDS.tag, AD_SECONDS.tag, true))
                        .addSubcommand(C_GUILD_COOLDOWN.tag,
                                Parameter.createCommand("setGuildCooldown"),
                                Parameter.createInput(A_COMMAND_NAME.tag, AD_COMMAND_NAME.tag, true),
                                Parameter.createInput(A_SECONDS.tag, AD_SECONDS.tag, true))
                        .build(),
                CommandCategory.BOT_CONFIG);
    }


    @Override
    public void execute(String label, String[] args, EventWrapper wrapper) {
        Optional<Command> command = parser.getCommand(args[1]);
        String cmd = args[0];

        if (command.isEmpty()) {
            MessageSender.sendSimpleError(ErrorType.COMMAND_SEARCH_EMPTY, wrapper);
            return;
        }

        if (isSubCommand(cmd, 0)) {
            setNsfw(args, command.get(), wrapper);
            return;
        }

        if (isSubCommand(cmd, 1)) {
            setAdminOnly(args, command.get(), wrapper);
            return;
        }
        if (isSubCommand(cmd, 2) || isSubCommand(cmd, 3)) {
            setCooldown(cmd, args, command.get(), wrapper);
            return;
        }
    }

    private void setCooldown(String cmd, String[] args, Command context,
                             EventWrapper wrapper) {
        OptionalInt seconds = ArgumentParser.parseInt(args[2]);


        if (seconds.isEmpty()) {
            MessageSender.sendSimpleError(ErrorType.NOT_A_NUMBER, wrapper);
            return;
        }

        boolean userCooldown = isSubCommand(cmd, 2);

        if (userCooldown) {
            if (!commandData.setUserCooldown(context, seconds.getAsInt(), wrapper)) {
                return;
            }
        } else {
            if (!commandData.setGuildCooldown(context, seconds.getAsInt(), wrapper)) {
                return;
            }
        }
        MessageSender.sendMessage(TextLocalizer.localizeAllAndReplace(
                "**" + (userCooldown ? M_SET_USER_COOLDOWN.tag : M_SET_GUILD_COOLDOWN.tag) + "**",
                wrapper, "\"" + context.getCommandName().toUpperCase() + "\"",
                seconds.getAsInt() + ""), wrapper.getMessageChannel());
    }


    private void setAdminOnly(String[] args, Command command, EventWrapper wrapper) {
        BooleanState bState = ArgumentParser.getBoolean(args[2]);

        if (bState == BooleanState.UNDEFINED) {
            MessageSender.sendSimpleError(ErrorType.INVALID_BOOLEAN, wrapper);
            return;
        }

        boolean state = bState.stateAsBoolean;

        if (!commandData.setAdmin(command, state, wrapper)) {
            return;
        }

        if (state) {
            MessageSender.sendMessage(TextLocalizer.localizeAllAndReplace("**" + M_ACTIVATED_ADMIN + "**",
                    wrapper, "\"" + command.getCommandName().toUpperCase() + "\""),
                    wrapper.getMessageChannel());

        } else {
            MessageSender.sendMessage(TextLocalizer.localizeAllAndReplace("**" + M_DEACTIVATED_ADMIN + "**",
                    wrapper, "\"" + command.getCommandName().toUpperCase() + "\""),
                    wrapper.getMessageChannel());
        }
    }

    private void setNsfw(String[] args, Command command, EventWrapper wrapper) {
        BooleanState bState = ArgumentParser.getBoolean(args[2]);

        if (bState == BooleanState.UNDEFINED) {
            MessageSender.sendSimpleError(ErrorType.INVALID_BOOLEAN, wrapper);
            return;
        }

        boolean state = bState.stateAsBoolean;

        if (!commandData.setNsfw(command, state, wrapper)) {
            return;
        }

        if (state) {
            MessageSender.sendMessage(TextLocalizer.localizeAllAndReplace("**" + M_ACTIVATED_NSFW + "**",
                    wrapper, "\"" + command.getCommandName().toUpperCase() + "\""),
                    wrapper.getMessageChannel());

        } else {
            MessageSender.sendMessage(TextLocalizer.localizeAllAndReplace("**" + M_DEACTIVATED_NSFW + "**",
                    wrapper, "\"" + command.getCommandName().toUpperCase() + "\""),
                    wrapper.getMessageChannel());
        }
    }

    @Override
    public void addParser(ArgumentParser parser) {
        this.parser = parser;
    }

    @Override
    public void addDataSource(DataSource source) {
        commandData = new CommandData(source);
    }

}
