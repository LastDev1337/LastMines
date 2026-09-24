package ru.last.mines.commands;

import dev.by1337.cmd.Argument;
import dev.by1337.cmd.ArgumentMap;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.CommandReader;
import dev.by1337.cmd.SuggestionsList;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.function.Supplier;

public final class SuggestingArgument extends Argument<CommandSender, String> {
    private final Supplier<Collection<String>> values;

    public SuggestingArgument(String name, Supplier<Collection<String>> values) {
        super(name);
        this.values = values;
    }

    @Override
    public void parse(CommandSender ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String s = reader.readString();
        if (!s.isEmpty()) out.put(name, s);
    }

    @Override
    public void suggest(CommandSender ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        reader.readString();
        values.get().forEach(suggestions::suggest);
    }

    @Override
    public boolean compilable() {
        return true;
    }

    @Override
    public boolean allowAsync() {
        return true;
    }
}
