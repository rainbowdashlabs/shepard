package de.eldoria.shepard.contexts.commands.util;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import de.eldoria.shepard.ShepardBot;
import de.eldoria.shepard.collections.LatestCommandsCollection;
import de.eldoria.shepard.contexts.ContextCategory;
import de.eldoria.shepard.contexts.commands.ArgumentParser;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.contexts.commands.argument.Parameter;
import de.eldoria.shepard.contexts.commands.argument.SubCommand;
import de.eldoria.shepard.localization.enums.commands.util.FeedbackLocale;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import okhttp3.OkHttpClient;

import static de.eldoria.shepard.localization.enums.commands.GeneralLocale.A_MESSAGE;

public class Feedback extends Command {
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().build();
    private WebhookClient webhookClient;

    /**
     * Create a new feedback command.
     */
    public Feedback() {
        super("feedback",
                new String[] {"bugreport"},
                FeedbackLocale.DESCRIPTION.tag,
                SubCommand.builder("feedback")
                        .addSubcommand(null,
                                Parameter.createInput(A_MESSAGE.tag, null, true))
                        .build(),
                ContextCategory.UTIL);

        webhookClient = new WebhookClientBuilder(ShepardBot.getConfig().getWebhooks().getFeedback())
                .setDaemon(true)
                .setHttpClient(OK_HTTP_CLIENT)
                .setWait(false)
                .build();
    }

    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(
                        messageContext.getAuthor().getAsTag() + " (" + messageContext.getAuthor().getId() + ")",
                        messageContext.getAuthor().getEffectiveAvatarUrl(), ""))
                .setDescription(ArgumentParser.getMessage(args, 0))
                .setTitle(new WebhookEmbed.EmbedTitle("New "
                        + (label.equalsIgnoreCase("bugreport") ? "Bugreport" : "Feedback") + " by "
                        + messageContext.getAuthor().getAsTag()
                        + " from Guild "
                        + messageContext.getGuild().getName(), ""));
        if (label.equalsIgnoreCase("bugreport")) {
            LatestCommandsCollection.SavedCommand latestCommand =
                    LatestCommandsCollection.getInstance()
                            .getLatestCommand(messageContext.getGuild(), messageContext.getAuthor());
            if (latestCommand != null) {
                builder.addField(new WebhookEmbed.EmbedField(false, "Last Command",
                        latestCommand.getLabel() + " " + String.join(" ", latestCommand.getArgs())));
            }
        }
        webhookClient.send(builder.build());

        MessageSender.sendMessage(FeedbackLocale.M_THANK_YOU.tag,
                messageContext.getTextChannel());
    }
}
