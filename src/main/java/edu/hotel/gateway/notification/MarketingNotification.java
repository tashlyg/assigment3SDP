package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.MessageSender;

import java.util.Objects;

/**
 * Independently added Refined Abstraction: formats a hotel offer.
 * It works with every existing MessageSender without modifying those providers.
 */
public final class MarketingNotification extends Notification {
    public MarketingNotification(MessageSender sender) {
        super(sender);
    }

    @Override
    public void send(String recipient, String message) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(message, "message must not be null");

        if (!sender.isAvailable()) {
            System.out.println("Marketing notification failed: " + sender.getChannelName() + " is unavailable.");
            return;
        }

        String offer = "[OFFER] " + message + "\nTo opt out, contact the front desk.";
        boolean delivered = sender.sendMessage(recipient, "Hotel offers", offer);
        System.out.println("Marketing notification " + (delivered ? "delivered" : "failed")
                + " via " + sender.getChannelName() + ".");
    }
}
