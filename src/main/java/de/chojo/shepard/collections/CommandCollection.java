package de.chojo.shepard.collections;

import de.chojo.shepard.contexts.commands.Command;

import java.util.ArrayList;

public class CommandCollection {
    private static CommandCollection instance;

    public static CommandCollection getInstance() {
        if (instance == null) {
            synchronized (CommandCollection.class) {
                if (instance == null) {
                    instance = new CommandCollection();
                }
            }
        }
        return instance;
    }

    private CommandCollection() {
    }

    private final ArrayList<Command> commands = new ArrayList<>();

    public void addCommand(Command command) {
        commands.add(command);
    }


    public ArrayList<Command> getCommands() {
        return commands;
    }

    public Command getCommand(String command) {
        for (Command currentCommand : commands) {
            if (currentCommand.isCommand(command)) {
                return currentCommand;
            }


        }
        return null;
    }

    public void debug() {
        System.out.println("++++ DEBUG OF COMMANDS ++++");
        for (Command c : commands) {
            c.printDebugInfo();
        }
    }
}
