package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.MessageSender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small test double: records calls without depending on real providers or console output. */
final class RecordingSender implements MessageSender {
    final List<SentMessage> calls = new ArrayList<>();
    final Set<String> rejectedRecipients = new HashSet<>();
    boolean available = true;
    boolean deliverySucceeds = true;
    int availabilityChecks = 0;
    private final String channelName;

    RecordingSender(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        calls.add(new SentMessage(recipient, title, body));
        return available && deliverySucceeds && !rejectedRecipients.contains(recipient);
    }

    @Override
    public boolean isAvailable() {
        availabilityChecks++;
        return available;
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    record SentMessage(String recipient, String title, String body) { }
}
