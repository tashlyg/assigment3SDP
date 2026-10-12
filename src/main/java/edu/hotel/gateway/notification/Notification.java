package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.MessageSender;

import java.util.Objects;

/**
 * Bridge Abstraction: notification behavior depends only on MessageSender.
 * The same notification can use a different sender while the program is running.
 */
public abstract class Notification {
    protected MessageSender sender;

    protected Notification(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    public void setSender(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    public abstract void send(String recipient, String message);
}
