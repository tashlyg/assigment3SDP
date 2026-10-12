package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.MessageSender;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Refined Abstraction: transforms a message before handing it to a sender.
 * Base64 is reversible encoding for this assignment, NOT secure encryption.
 * Do not use this demonstration to protect passwords or other sensitive data.
 */
public final class EncryptedNotification extends Notification {
    public EncryptedNotification(MessageSender sender) {
        super(sender);
    }

    @Override
    public void send(String recipient, String message) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(message, "message must not be null");

        if (!sender.isAvailable()) {
            System.out.println("Encoded notification failed: " + sender.getChannelName() + " is unavailable.");
            return;
        }

        String encodedMessage = Base64.getEncoder()
                .encodeToString(message.getBytes(StandardCharsets.UTF_8));
        boolean delivered = sender.sendMessage(recipient, "Encoded hotel message", encodedMessage);
        System.out.println("Encoded notification " + (delivered ? "delivered" : "failed")
                + " via " + sender.getChannelName() + ".");
    }
}
