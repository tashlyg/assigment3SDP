package edu.hotel.gateway.sender;

import java.io.PrintStream;
import java.util.Objects;

/** A new simulated channel added without changing any existing notification. */
public final class PushNotificationSender implements MessageSender {
    private boolean available;
    private final PrintStream output;

    public PushNotificationSender() {
        this(true);
    }

    public PushNotificationSender(boolean available) {
        this(available, System.out);
    }

    public PushNotificationSender(boolean available, PrintStream output) {
        this.available = available;
        this.output = Objects.requireNonNull(output, "output must not be null");
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available || !SenderSupport.isValidMessage(recipient, body)) {
            return false;
        }

        output.println("[Push] To: " + recipient);
        output.println("Title: " + SenderSupport.normalizeTitle(title));
        output.println(body);
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
        return "Push";
    }
}
