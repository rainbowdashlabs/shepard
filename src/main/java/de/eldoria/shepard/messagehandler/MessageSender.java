package de.eldoria.shepard.messagehandler;

import de.eldoria.shepard.ShepardBot;
import de.eldoria.shepard.database.types.GreetingSettings;
import de.eldoria.shepard.util.Replacer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.awt.Color;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class MessageSender {

    /**
     * send a simple Message to a channel.
     *
     * @param message Message to send.
     * @param channel channel to send
     */
    public static void sendMessage(String message, MessageChannel channel) {
        if (message.isEmpty()) return;

        String[] messageParts = message.split(System.lineSeparator());
        StringBuilder messagePart = new StringBuilder();
        for (int i = 0; i < messageParts.length; i++) {
            if (messagePart.length() + messageParts[i].length() < 1024) {
                messagePart.append(messageParts[i]).append(System.lineSeparator());
            } else {
                channel.sendMessage(messagePart.toString()).queue();
                messagePart = new StringBuilder();
                i--;
            }
        }

        channel.sendMessage(messagePart.toString()).queue();
    }

    /**
     * Sends a textbox to a channel.
     *
     * @param title   Title of the chatbox.
     * @param fields  List of fields for the chatbox.
     * @param channel channel to send.
     */
    public static void sendTextBox(String title, List<MessageEmbed.Field> fields, MessageChannel channel) {
        sendTextBox(title, fields, channel, Color.gray);
    }

    /**
     * Sends a textbox to a channel.
     *
     * @param title   Title of the chatbox.
     * @param fields  List of fields for the chatbox.
     * @param channel channel to send.
     * @param color   Color of the text box
     */
    public static void sendTextBox(String title, List<MessageEmbed.Field> fields, MessageChannel channel, Color color) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription("test");
        builder.setTitle(title);
        builder.setColor(color);
        for (MessageEmbed.Field field : fields) {
            builder.addField(field);
        }
        builder.setFooter("by Shepard", ShepardBot.getJDA().getSelfUser().getAvatarUrl());
        channel.sendMessage(builder.build()).queue();
    }

    /**
     * Send a simple text box with title and text.
     *
     * @param title       Title of text box
     * @param description Text of textbox
     * @param channel     channel to send
     */
    public static void sendSimpleTextBox(String title, String description, MessageChannel channel) {
        sendSimpleTextBox(title, description, Color.gray, channel);
    }

    /**
     * Send a simple text box with title and text.
     *
     * @param title       Title of text box
     * @param description Text of textbox
     * @param channel     channel to send
     * @param color       Color of the text box
     */
    public static void sendSimpleTextBox(String title, String description, Color color, MessageChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(title)
                .setColor(color)
                .setDescription(description)
                .setFooter("by Shepard", ShepardBot.getJDA().getSelfUser().getAvatarUrl());
        channel.sendMessage(builder.build()).queue();
    }

    /**
     * Sends a error with text box.
     *
     * @param fields  List of fields.
     * @param channel channel to send.
     */
    public static void sendError(MessageEmbed.Field[] fields, MessageChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("ERROR!");
        for (MessageEmbed.Field field : fields) {
            builder.addField(field);
            builder.setColor(Color.red);
            builder.setFooter("by Shepard", ShepardBot.getJDA().getSelfUser().getAvatarUrl());
            channel.sendMessage(builder.build()).queue();
        }
    }

    /**
     * Sends a simple error with predefined error messages.
     *
     * @param type    error type
     * @param channel channel to send
     */
    public static void sendSimpleError(ErrorType type, MessageChannel channel) {
        sendSimpleError(type.message, channel);
    }

    /**
     * Sends a simple error to a channel.
     *
     * @param error   Error message
     * @param channel channel to send
     */
    public static void sendSimpleError(String error, MessageChannel channel) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("ERROR!")
                .setDescription(error)
                .setColor(Color.red)
                .setFooter("by Shepard", ShepardBot.getJDA().getSelfUser().getAvatarUrl());
        try {
            channel.sendMessage(builder.build()).queue();
        } catch (ErrorResponseException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes a received message.
     *
     * @param receivedEvent Event of message receive
     */
    public static void deleteMessage(MessageReceivedEvent receivedEvent) {
        try {
            receivedEvent.getMessage().delete().submit();
        } catch (InsufficientPermissionException e) {
            MessageSender.sendError(new MessageEmbed.Field[] {new MessageEmbed.Field("Lack of Permission",
                    "Missing permission: MESSAGE_MANAGE", false)}, receivedEvent.getChannel());
        }
    }

    /**
     * Loggs a message in plain text.
     *
     * @param event   event to log
     * @param channel channel to log
     */
    public static void logMessageAsPlainText(MessageReceivedEvent event, MessageChannel channel) {
        channel.sendMessage(event.getGuild().getName() + " | " + event.getMessage().getCategory().getName()
                + " | " + event.getMessage().getChannel().getName() + " by " + event.getAuthor().getName()
                + ": " + event.getMessage().getContentRaw()).queue();
    }

    /**
     * Loggs a message es embed.
     *
     * @param event   event to log
     * @param channel channel to log
     */
    public static void logMessageAsEmbedded(MessageReceivedEvent event, MessageChannel channel) {
        Instant instant = Instant.now(); // get The current time in instant object
        Timestamp t = java.sql.Timestamp.from(instant); // Convert instant to Timestamp

        if (event.getChannel() instanceof TextChannel) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(event.getGuild().getName() + " | " + event.getChannel().getName());
            builder.setTimestamp(t.toInstant());
            builder.setAuthor(event.getAuthor().getAsTag(), null, event.getAuthor().getAvatarUrl());
            builder.setDescription(event.getMessage().getContentRaw());
            channel.sendMessage(builder.build()).queue();
        }
    }

    /**
     * sends a greeting text.
     *
     * @param event    event to log
     * @param channel  channel to log
     * @param source   invite source
     * @param greeting Greeting object
     */
    public static void sendGreeting(GuildMemberJoinEvent event, GreetingSettings greeting,
                                    String source, MessageChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setThumbnail(event.getUser().getAvatarUrl());
        if (source != null) {
            builder.setFooter("Joined via " + source);

        }
        User user = event.getUser();
        String message = Replacer.applyUserPlaceholder(user, greeting.getText());
        builder.addField(event.getMember().getUser().getAsTag(),
                message, true);
        channel.sendMessage((builder.build())).queue();
    }
}