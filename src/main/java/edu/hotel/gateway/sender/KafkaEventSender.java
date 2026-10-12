package edu.hotel.gateway.sender;

import java.io.PrintStream;
import java.util.Objects;

/** Prints a JSON event to a simulated Kafka topic; no broker is contacted. */
public final class KafkaEventSender implements MessageSender {
    public static final String TOPIC = "hotel.notifications";

    private boolean available;
    private final PrintStream output;

    public KafkaEventSender() {
        this(true);
    }

    public KafkaEventSender(boolean available) {
        this(available, System.out);
    }

    public KafkaEventSender(boolean available, PrintStream output) {
        this.available = available;
        this.output = Objects.requireNonNull(output, "output must not be null");
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available || !SenderSupport.isValidMessage(recipient, body)) {
            return false;
        }

        String event = "{\"recipient\":\"" + SenderSupport.escapeJson(recipient)
                + "\",\"title\":\"" + SenderSupport.escapeJson(SenderSupport.normalizeTitle(title))
                + "\",\"body\":\"" + SenderSupport.escapeJson(body) + "\"}";
        output.println("[Kafka] topic=" + TOPIC + " event=" + event);
        return true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getChannelName() {
        return "Kafka";
    }
}
