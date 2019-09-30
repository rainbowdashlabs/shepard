package de.eldoria.shepard.contexts.commands.admin;

import de.eldoria.shepard.ShepardBot;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.CommandArg;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.util.TextFormatting;
import de.eldoria.shepard.util.Verifier;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.internal.entities.ActivityImpl;

import static de.eldoria.shepard.util.Verifier.isArgument;
import static java.lang.System.lineSeparator;

public class BotPresence extends Command {

    public BotPresence() {
        commandName = "presence";
        commandDesc = "Set Shepards presence";
        commandArgs = new CommandArg[] {
                new CommandArg("action",
                        "**__p__laying** -> set the playing status" + lineSeparator()
                                + "**__s__treaming** -> set the streaming status" + lineSeparator()
                                + "**__l__istening** -> set the listening status" + lineSeparator()
                                + "**__c__lear** -> set the normal online status.",
                        true),
                new CommandArg("values",
                        "**__p__laying** -> [title]" + lineSeparator()
                                + "**__s__treaming** -> [title] [twitch url]" + lineSeparator()
                                + "**__l__istening** -> [title]" + lineSeparator()
                                + "**__c__lear** -> leave empty.",
                        false
                )
        };
    }

    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        if (args.length < 1) {
            MessageSender.sendSimpleError(ErrorType.TOO_FEW_ARGUMENTS, messageContext.getChannel());
            return;
        }
        Presence presence = ShepardBot.getJDA().getPresence();

        String activity = args[0];

        if (isArgument(activity, "clear", "c")) {
            presence.setActivity(null);
            return;
        }

        if (args.length < 2) {
            MessageSender.sendSimpleError(ErrorType.TOO_FEW_ARGUMENTS, messageContext.getChannel());
            return;
        }

        if (isArgument(activity, "playing", "p")) {
            presence.setActivity(Activity.playing(
                    TextFormatting.getRangeAsString(" ", args, 1, args.length)
            ));
            return;
        }
        if (isArgument(activity, "streaming", "s")) {
            if (args.length > 2) {

                presence.setActivity(Activity.streaming(
                        TextFormatting.getRangeAsString(" ", args, 1, args.length - 1),
                        args[args.length - 1]
                ));
                return;
            } else {
                MessageSender.sendSimpleError(ErrorType.TOO_FEW_ARGUMENTS, messageContext.getChannel());
            }
        }

        //Not supported yet
        /*if (isArgument(activity, "watching", "w")) {
            presence.setActivity(Activity.watching(
                    TextFormatting.getRangeAsString(" ", args, 1, args.length)
            ));
            return;
        }*/

        if (isArgument(activity, "listening", "l")) {
            presence.setActivity(Activity.listening(
                    TextFormatting.getRangeAsString(" ", args, 1, args.length)
            ));
            return;
        }


        MessageSender.sendSimpleError(ErrorType.INVALID_ACTION, messageContext.getChannel());


    }
}
