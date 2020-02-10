package de.eldoria.shepard.listener;

import de.eldoria.shepard.localization.util.LocalizedEmbedBuilder;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.minigames.guessgame.ConfigurationState;
import de.eldoria.shepard.minigames.guessgame.ImageConfiguration;
import de.eldoria.shepard.minigames.guessgame.ImageRegister;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;

import static de.eldoria.shepard.localization.enums.listener.GuessGameImageRegisterListenerLocale.*;

public class GuessGameImageRegisterListener extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        MessageEventDataWrapper wrapper = new MessageEventDataWrapper(event);
        ImageRegister register = ImageRegister.getInstance();
        ConfigurationState configurationState = register.getConfigurationState(wrapper);
        if (configurationState != ConfigurationState.NONE) {
            if (event.getMessage().getAttachments().size() == 1) {
                if (configurationState == ConfigurationState.CROPPED) {
                    MessageSender.sendMessage(M_COPPED_REGISTERED.tag, event.getChannel());
                }
                register.addImage(wrapper,
                        event.getMessage().getAttachments().get(0).getUrl());

            }
        }

        if (register.getConfigurationState(wrapper) == ConfigurationState.CONFIGURED) {
            ImageConfiguration configuration = register.getConfiguration(wrapper);

            if (register.registerConfiguration(wrapper)) {
                LocalizedEmbedBuilder builder = new LocalizedEmbedBuilder(event.getGuild())
                        .setTitle(configuration.isNsfw() ? M_ADDED_NSFW.tag : M_ADDED_SFW.tag)
                        .setThumbnail(configuration.getCroppedImage())
                        .setImage(configuration.getFullImage())
                        .setDescription(M_SET_REGISTERED.tag)
                        .setColor(Color.green);

                event.getChannel().sendMessage(builder.build()).queue();

            }
        }
    }
}