package ru.last.mines.commands;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
    String name();
    String[] aliases() default {};
}
