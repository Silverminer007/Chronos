package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Person;

public interface IMessageSender {
    void sendMessage(String message, Person sendTo) throws FriendlyError;
}