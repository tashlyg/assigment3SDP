package edu.hotel.gateway.sender;

import java.io.PrintStream;
import java.util.Objects;

/** A simulated SMS channel. The exercise limit is 160 Unicode code points. */
public final class SmsMessageSender implements MessageSender {
    public static final int MAX_LENGTH = 160;

    private boolean available;
    private final PrintStream output;

    public SmsMessageSender() {
        this(true);
    }

    public SmsMessageSender(boolean available) {
        this(available, System.out);
    }

    public SmsMessageSender(boolean available, PrintStream output) {
        this.available = available;
        this.output = Objects.requireNonNull(output, "output must not be null");
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available || !SenderSupport.isValidMessage(recipient, body)) {
            return false;
        }
        if (body.codePointCount(0, body.length()) > MAX_LENGTH) {
            return false;
        }

        // The SMS channel deliberately ignores the title.
        output.println("[SMS] To: " + recipient);
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
        return "SMS";
    }
}
