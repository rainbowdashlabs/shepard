package de.eldoria.shepard.contexts.commands.fun;

import de.eldoria.shepard.contexts.ContextCategory;
import de.eldoria.shepard.contexts.commands.Command;
import de.eldoria.shepard.database.queries.KudoData;
import de.eldoria.shepard.localization.enums.fun.KudoLotteryLocale;
import de.eldoria.shepard.messagehandler.ErrorType;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.minigames.ChannelEvaluator;
import de.eldoria.shepard.minigames.Evaluator;
import de.eldoria.shepard.minigames.kudolottery.KudoLotteryEvaluator;
import de.eldoria.shepard.util.reactions.EmoteCollection;
import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;

import static de.eldoria.shepard.localization.enums.fun.KudoLotteryLocale.*;

public class KudoLottery extends Command {
    public KudoLottery() {
        commandName = "kudoLottery";
        commandAliases = new String[] {"lottery", "kl"};
        commandDesc = DESCRIPTION.replacement;
        category = ContextCategory.FUN;
    }

    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        boolean success = KudoData.tryTakePoints(messageContext.getGuild(),
                messageContext.getAuthor(), 1, messageContext);

        if (!success) {
            MessageSender.sendSimpleError(ErrorType.NOT_ENOUGH_KUDOS, messageContext);
            return;
        }

        ChannelEvaluator<KudoLotteryEvaluator> kudoLotteryScheduler
                = Evaluator.getKudoLotteryScheduler();

        if (kudoLotteryScheduler.isEvaluationActive(messageContext.getTextChannel())) {
            MessageSender.sendMessage(M_LOTTERY_RUNNING.replacement, messageContext);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(M_EMBED_TITLE.replacement)
                .setDescription(M_EMBED_DESCRIPTION.replacement)
                .addField(M_EMBED_KUDOS_IN_POT.replacement,
                        locale.getReplacedString(M_EMBED_EXPLANATION.localeCode,
                                messageContext.getGuild(),
                                EmoteCollection.INFINITY.getEmote().getAsMention(),
                                EmoteCollection.PLUS_X.getEmote().getAsMention(),
                                EmoteCollection.PLUS_I.getEmote().getAsMention()),
                        true)
                .setColor(Color.orange);

        messageContext.getChannel().sendMessage(builder.build()).queue(message -> {
            message.addReaction(EmoteCollection.INFINITY.getEmote()).queue();
            message.addReaction(EmoteCollection.PLUS_X.getEmote()).queue();
            message.addReaction(EmoteCollection.PLUS_I.getEmote()).queue();
            kudoLotteryScheduler.scheduleEvaluation(message, 180,
                    new KudoLotteryEvaluator(message, messageContext.getAuthor()));
        });
    }
}
