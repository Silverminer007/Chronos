package de.henzeob.chronos.exceptions;

public class CaldavException extends Exception {
    private final String responseBody;

    public CaldavException(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
