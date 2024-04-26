package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Person;

public class MultiMessageSender implements IMessageSender {
    private final IMessageSender[] senders;

    public MultiMessageSender(IMessageSender... senders) {
        this.senders = senders;
    }


    @Override
    public void sendMessage(String message, Person sendTo) throws FriendlyError {
        for(IMessageSender sender : senders) {
            sender.sendMessage(message, sendTo);
        }
    }
}