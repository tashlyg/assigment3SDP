package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.MessageSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Refined Abstraction: urgent formatting and provider-independent fallback. */
public final class UrgentNotification extends Notification {
    private final List<MessageSender> fallbackSenders = new ArrayList<>();

    public UrgentNotification(MessageSender primary, MessageSender... fallbackSenders) {
        super(primary);
        Objects.requireNonNull(fallbackSenders, "fallbackSenders must not be null");
        for (MessageSender fallback : fallbackSenders) {
            addFallbackSender(fallback);
        }
    }

    public void addFallbackSender(MessageSender fallbackSender) {
        fallbackSenders.add(Objects.requireNonNull(
                fallbackSender, "fallbackSender must not be null"));
    }

    @Override
    public void send(String recipient, String message) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(message, "message must not be null");
        String urgentMessage = "[URGENT] " + message;

        if (trySend(sender, recipient, urgentMessage)) {
            return;
        }

        for (MessageSender fallback : fallbackSenders) {
            if (trySend(fallback, recipient, urgentMessage)) {
                return;
            }
        }

        System.out.println("Urgent notification failed: no provider delivered the message.");
    }

    private boolean trySend(MessageSender candidate, String recipient, String message) {
        if (!candidate.isAvailable()) {
            System.out.println(candidate.getChannelName() + " is unavailable; trying next provider.");
            return false;
        }

        boolean delivered = candidate.sendMessage(recipient, "Hotel alert", message);
        if (delivered) {
            System.out.println("Urgent notification delivered via " + candidate.getChannelName() + ".");
        } else {
            System.out.println(candidate.getChannelName() + " rejected delivery; trying next provider.");
        }
        return delivered;
    }
}
