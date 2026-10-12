package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.MessageSender;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Refined Abstraction: sends the same message to a saved collection of recipients.
 * The caller starts the batch at the chosen time. This simulation has no timer,
 * background thread, or automatic scheduling requirement.
 */
public final class ScheduledBatchNotification extends Notification {
    private final List<String> recipients;
    private BatchSummary lastSummary = new BatchSummary(0, 0, 0);

    public ScheduledBatchNotification(MessageSender sender, Collection<String> recipients) {
        super(sender);
        this.recipients = List.copyOf(Objects.requireNonNull(
                recipients, "recipients must not be null"));
    }

    /** Sends to the defensive copy of recipients provided in the constructor. */
    public void send(String message) {
        dispatch(recipients, message);
    }

    /**
     * Honors the base class contract: this overload sends to one explicit recipient.
     * Use send(message) to send to the complete saved collection.
     */
    @Override
    public void send(String recipient, String message) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        dispatch(List.of(recipient), message);
    }

    public BatchSummary getLastSummary() {
        return lastSummary;
    }

    private void dispatch(List<String> targetRecipients, String message) {
        Objects.requireNonNull(message, "message must not be null");
        int successful = 0;

        for (String recipient : targetRecipients) {
            // An available provider may still reject an individual message.
            boolean delivered = sender.isAvailable()
                    && sender.sendMessage(recipient, "Hotel guest update", message);
            if (delivered) {
                successful++;
            } else {
                System.out.println("Batch delivery failed for " + recipient + ".");
            }
        }

        int total = targetRecipients.size();
        lastSummary = new BatchSummary(total, successful, total - successful);
        System.out.println("Total: " + lastSummary.total());
        System.out.println("Successful: " + lastSummary.successful());
        System.out.println("Failed: " + lastSummary.failed());
    }
}
