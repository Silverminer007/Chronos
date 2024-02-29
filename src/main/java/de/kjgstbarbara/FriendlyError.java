package de.kjgstbarbara;

import lombok.Getter;

@Getter
public class FriendlyError extends Exception {
    private final String message;

    public FriendlyError(String message) {
        this.message = message;
    }
}