package de.eldoria.shepard.contexts.commands.fun;

import de.eldoria.shepard.contexts.commands.ArgumentParser;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.argument.CommandArgument;
import de.eldoria.shepard.contexts.commands.argument.SubArgument;
import de.eldoria.shepard.database.queries.KudoData;
import de.eldoria.shepard.localization.enums.commands.GeneralLocale;
import de.eldoria.shepard.localization.enums.commands.fun.KudoGambleLocale;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.util.reactions.Emoji;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static de.eldoria.shepard.localization.util.TextLocalizer.localizeAll;
import static de.eldoria.shepard.localization.util.TextLocalizer.localizeAllAndReplace;

/**
 * Creates a gamble game.
 * A gamble game can be started by every user per channel.
 * The gamble game goes over three round. Every round forces a message edit.
 * The result is pseudo random.
 * This command requires asynchronous execution
 */
public class KudoGamble extends Command {
    private final int bonus = 64;
    private final int tier1 = 16;
    private final int tier2 = 4;
    private final int tier3 = 1;
    private final int tier4 = 0;
    private Random random = new Random();

    /**
     * Create a new Kudo Gamble.
     */
    public KudoGamble() {
        commandName = "kudoGamble";
        commandAliases = new String[] {"gamble"};
        commandDesc = KudoGambleLocale.DESCRIPTION.tag;
        commandArguments = new CommandArgument[] {new CommandArgument("amount", true,
                new SubArgument("amount", GeneralLocale.A_AMOUNT.tag))};
    }


    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        Message message;
        Integer amount = ArgumentParser.parseInt(args[0]);
        if (amount == null || amount < 1) {
            MessageSender.sendSimpleError(ErrorType.NOT_A_NUMBER, messageContext.getTextChannel());
            return;
        }
        if (!KudoData.tryTakePoints(messageContext.getGuild(), messageContext.getAuthor(), amount, messageContext)) {
            MessageSender.sendSimpleError(ErrorType.NOT_ENOUGH_KUDOS, messageContext.getTextChannel());
            return;
        }
        MessageChannel channel = messageContext.getChannel();
        StringBuilder messageText = new StringBuilder(
                localizeAllAndReplace(KudoGambleLocale.M_START.tag, messageContext.getGuild(),
                        "**" + messageContext.getMember().getEffectiveName() + "**"));
        message = channel.sendMessage(messageText.toString()).complete();

        messageText.append(System.lineSeparator()).append("**" + localizeAll(KudoGambleLocale.M_GAMBLE.tag, messageContext.getGuild()) + "**").append(System.lineSeparator());

        String square = Emoji.BLACK_LARGE_SQUARE.unicode;

        message.editMessage(messageText.toString() + square + " " + square + " " + square).complete();

        int win1 = getRandomTier();
        int win2 = getRandomTier();
        int win3 = getRandomTier();

        String finalMessageText;
        try {
            Thread.sleep(2000);
            message.editMessage(messageText.toString() + getEmoji(win1)
                    + " " + square + " " + square).complete();
            Thread.sleep(2000);
            message.editMessage(messageText.toString() + getEmoji(win1) + " "
                    + getEmoji(win2) + " " + square).complete();
            Thread.sleep(2000);
            finalMessageText = messageText.toString() + getEmoji(win1) + " "
                    + getEmoji(win2) + " " + getEmoji(win3);
            message.editMessage(finalMessageText).complete();
        } catch (InterruptedException e) {
            KudoData.addRubberPoints(messageContext.getGuild(), messageContext.getAuthor(), amount, messageContext);
            return;
        }

        int winAmount = amount;
        int winId = (win1) + (win2) + (win3);
        boolean jackpot = false;


        if (winId == tier4 * 3) {
            winAmount *= 50;
        } else if (winId == tier3 * 3) {
            winAmount *= 25;
        } else if (winId == tier2 * 3) {
            winAmount *= 10;
        } else if (winId == tier1 * 3) {
            winAmount = amount * 5;
        } else if (winId == bonus * 3) {
            winAmount = amount + KudoData.getAndClearJackpot(messageContext.getGuild(), messageContext);
            jackpot = true;
        } else if (win1 == bonus || win2 == bonus || win3 == bonus) {
            //Bonus
            winAmount = (int) Math.round(amount * 1.5);
        } else if (win1 == win2 || win2 == win3) {
            //Two are equal
            winAmount = (int) Math.round(amount * evaluatePairs(win2));
        } else if (win1 == win3) {
            winAmount = (int) Math.round(amount * evaluatePairs(win1));
        } else {
            winAmount = 0;
        }

        if (winAmount == 0) {
            int jackpotAmount = KudoData.addAndGetJackpot(messageContext.getGuild(), amount, messageContext);
            message.editMessage(finalMessageText + System.lineSeparator()
                    + localizeAllAndReplace(KudoGambleLocale.M_LOSE.tag, messageContext.getGuild(),
                    "**" + jackpotAmount + "**")).complete();
            return;
        }

        if (winAmount < amount) {
            int jackpotAmount = KudoData.addAndGetJackpot(messageContext.getGuild(), amount - winAmount, messageContext);
            KudoData.addRubberPoints(messageContext.getGuild(), messageContext.getAuthor(), winAmount, messageContext);
            message.editMessage(finalMessageText + System.lineSeparator()
                    + localizeAllAndReplace(KudoGambleLocale.M_PART_LOSE.tag, messageContext.getGuild(),
                    "**" + winAmount + "**", "**" + jackpotAmount + "**")).complete();
            return;
        }

        if (!jackpot) {
            message.editMessage(finalMessageText + System.lineSeparator()
                    + localizeAllAndReplace(KudoGambleLocale.M_WIN.tag, messageContext.getGuild(),
                    "**" + winAmount + "**")).complete();
            KudoData.addRubberPoints(messageContext.getGuild(), messageContext.getAuthor(), winAmount, messageContext);
            return;
        }

        message.editMessage(finalMessageText + System.lineSeparator() + "**"
                + localizeAllAndReplace(KudoGambleLocale.M_JACKPOT.tag + "**", messageContext.getGuild(),
                winAmount + "")).complete();
        KudoData.addRubberPoints(messageContext.getGuild(), messageContext.getAuthor(), winAmount, messageContext);


    }

    private String getEmoji(int i) {
        switch (i) {
            case tier4:
                return Emoji.GEM.unicode;
            case tier3:
                return Emoji.DIAMAOND_SHAPE_WITH_DOT.unicode;
            case tier2:
                return Emoji.MONEY_BAG.unicode;
            case tier1:
                return Emoji.DOLLAR.unicode;
            case bonus:
                return Emoji.TADA.unicode;
            default:
                return Emoji.CROSS_MARK.unicode;
        }
    }

    private int getRandomTier() {
        int value = ThreadLocalRandom.current().nextInt(101);
        //Bonus
        // 2 of 100
        if (value >= 98) {
            return bonus;
        }
        //Tier 1
        //48 of 100
        if (value >= 50) {
            return tier1;
        }

        //Tier 2
        //25 of 100
        if (value >= 25) {
            return tier2;
        }

        //Tier 3
        //15 of 100
        if (value >= 10) {
            return tier3;
        }

        //Tier 4
        //10 of 100
        return tier4;
    }

    private double evaluatePairs(int tier) {
        switch (tier) {
            case bonus:
                return 5;
            case tier1:
                return 0.875;
            case tier2:
                return 2;
            case tier3:
                return 3;
            case tier4:
                return 2;
            default:
                return 0;
        }
    }
}