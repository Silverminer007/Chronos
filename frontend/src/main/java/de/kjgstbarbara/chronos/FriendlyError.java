package de.kjgstbarbara.chronos;

import lombok.Getter;

@Getter
public class FriendlyError extends Exception {
    private final String message;

    public FriendlyError(String message) {
        this.message = message;
    }
}