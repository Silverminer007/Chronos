package de.henzeob.chronos.entities;

import java.util.List;

public class Notification {
    String title;
    String body;
    List<Action> actions;

    static class Action {
        String text;
        Runnable action;
    }
}