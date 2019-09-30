package de.eldoria.shepard.contexts.commands.fun;

import de.eldoria.shepard.wrapper.MessageEventDataWrapper;
import de.eldoria.shepard.messagehandler.MessageSender;
import de.eldoria.shepard.contexts.commands.Command;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class RandomJoke extends Command {

    /**
     * Creates a new random joke command object.
     */
    public RandomJoke() {
        commandName = "joke";
        commandDesc = "Hehe";
    }

    @Override
    protected void internalExecute(String label, String[] args, MessageEventDataWrapper messageContext) {
        try {
            MessageSender.sendSimpleTextBox("Random Joke", getRandomJoke(), messageContext.getChannel());
        } catch (IOException e) {
            MessageSender.sendError(new MessageEmbed.Field[]{new MessageEmbed.Field("Error", e.getMessage(),
                    false)}, messageContext.getChannel());
        }
    }

    private String getRandomJoke() throws IOException {
        URL url = new URL("https://icanhazdadjoke.com/");
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setReadTimeout(5000);
        con.setConnectTimeout(5000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();
        con.disconnect();

        JSONObject jsonResponse = new JSONObject(response.toString());

        return jsonResponse.getString("joke");
    }
}