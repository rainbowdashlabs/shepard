package de.eldoria.shepard.contexts.commands.admin;

import de.eldoria.shepard.contexts.ContextCategory;
import de.eldoria.shepard.contexts.commands.ArgumentParser;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.argument.Parameter;
import de.eldoria.shepard.contexts.commands.argument.SubCommand;
import de.eldoria.shepard.database.queries.commands.QuoteData;
import de.eldoria.shepard.database.types.QuoteElement;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.AD_ID;
import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_ID;
import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_TEXT;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.AD_KEYWORD;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.A_KEYWORD;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.C_ADD;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.C_ALTER;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.C_LIST;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.C_REMOVE;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.DESCRIPTION;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.M_CHANGED_QUOTE;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.M_NO_QUOTES;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.M_REMOVED_QUOTE;
import static de.eldoria.shepard.localization.enums.commands.admin.ManageQuoteLocale.M_SAVED_QUOTE;
import static de.eldoria.shepard.localization.util.TextLocalizer.localizeAllAndReplace;
import static java.lang.System.lineSeparator;

/**
 * Command to add, remove, alter and list quotes.
 */
public class ManageQuote extends Command {

    /**
     * Create a new manage quote command object.
     */
    public ManageQuote() {
        super("manageQuotes",
                new String[] {"mq"},
                DESCRIPTION.tag,
                SubCommand.builder("manageQuotes")
                        .addSubcommand(C_ADD.tag,
                                Parameter.createCommand("create"),
                                Parameter.createInput(A_TEXT.tag, null, true))
                        .addSubcommand(C_ALTER.tag,
                                Parameter.createCommand("edit"),
                                Parameter.createInput(A_ID.tag, AD_ID.tag, true),
                                Parameter.createInput(A_TEXT.tag, null, true))
                        .addSubcommand(C_REMOVE.tag,
                                Parameter.createCommand("remove"),
                                Parameter.createInput(A_ID.tag, AD_ID.tag, true))
                        .addSubcommand(C_LIST.tag,
                                Parameter.createCommand("list"),
                                Parameter.createInput(A_KEYWORD.tag, AD_KEYWORD.tag, false))
                        .build(),
                ContextCategory.ADMIN);
    }

    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        String cmd = args[0];
        SubCommand arg = subCommands[0];
        if (isSubCommand(cmd, 0)) {
            create(args, messageContext);
            return;
        }

        if (isSubCommand(cmd, 1)) {
            alter(args, messageContext);
            return;
        }

        if (isSubCommand(cmd, 2)) {
            remove(args, messageContext);
            return;
        }

        if (isSubCommand(cmd, 3)) {
            list(args, messageContext);
            return;
        }

        MessageSender.sendSimpleError(ErrorType.INVALID_ACTION, messageContext.getTextChannel());
    }

    private void alter(String[] args, MessageEventDataWrapper messageContext) {
        if (args.length < 3) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, messageContext.getTextChannel());
        }

        int quoteId = verifyId(args[1], messageContext);

        if (quoteId == -1) {
            return;
        }

        String quote = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (QuoteData.alterQuote(messageContext.getGuild(), quoteId, quote, messageContext)) {
            MessageSender.sendSimpleTextBox(localizeAllAndReplace(M_CHANGED_QUOTE.tag,
                    messageContext.getGuild(), "**" + quoteId + "**"), quote, Color.blue,
                    messageContext.getTextChannel());
        }
    }

    private void list(String[] args, MessageEventDataWrapper messageContext) {
        List<QuoteElement> quotes;
        if (args.length > 1) {
            quotes = QuoteData.getQuotesByKeyword(messageContext.getGuild(),
                    String.join(" ", Arrays.copyOfRange(args, 1, args.length)), messageContext);

        } else {
            quotes = QuoteData.getQuotes(messageContext.getGuild(), messageContext);
        }

        if (quotes.size() == 0) {
            MessageSender.sendMessage(M_NO_QUOTES.tag, messageContext.getTextChannel());
        }

        String message = quotes.stream()
                .map(quote -> "**" + quote.getQuoteId() + "** -> " + quote.getQuote() + lineSeparator())
                .collect(Collectors.joining());
        MessageSender.sendMessage(message, messageContext.getTextChannel());
    }

    private void remove(String[] args, MessageEventDataWrapper messageContext) {
        if (args.length != 2) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ARGUMENT, messageContext.getTextChannel());
        }

        int quoteId = verifyId(args[1], messageContext);

        if (quoteId == -1) {
            return;
        }

        if (QuoteData.removeQuote(messageContext.getGuild(), quoteId, messageContext)) {
            MessageSender.sendMessage(localizeAllAndReplace(M_REMOVED_QUOTE.tag,
                    messageContext.getGuild(), "**" + quoteId + "**"), messageContext.getTextChannel());
        }
    }

    private void create(String[] args, MessageEventDataWrapper messageContext) {
        if (args.length == 1) {
            MessageSender.sendSimpleError(ErrorType.NO_QUOTE_FOUND, messageContext.getTextChannel());
            return;
        }

        String quote = ArgumentParser.getMessage(args, 1);

        if (QuoteData.addQuote(messageContext.getGuild(), quote, messageContext)) {
            MessageSender.sendSimpleTextBox(M_SAVED_QUOTE.tag, quote, Color.green, messageContext.getTextChannel());
        }
    }

    /**
     * Returns the id from a string.
     *
     * @param number         string to parse
     * @param messageContext message context for error logging
     * @return -1 when the string is not a number or the number is <0 or larger than the amount of quotes.
     */
    private int verifyId(String number, MessageEventDataWrapper messageContext) {
        int quotesCount = QuoteData.getQuotesCount(messageContext.getGuild(), messageContext);
        OptionalInt quoteId = ArgumentParser.parseInt(number);
        if (quoteId.isEmpty()) {
            MessageSender.sendSimpleError(ErrorType.NOT_A_NUMBER, messageContext.getTextChannel());
            return -1;
        }

        if (quoteId.getAsInt() > quotesCount || quoteId.getAsInt() < 0) {
            MessageSender.sendSimpleError(ErrorType.INVALID_ID, messageContext.getTextChannel());
            return -1;
        }
        return quoteId.getAsInt();
    }
}
