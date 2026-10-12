package edu.hotel.gateway.sender;

import java.io.PrintStream;
import java.util.Objects;

/** A simulated email channel with a subject and a hotel signature. */
public final class EmailMessageSender implements MessageSender {
    private boolean available;
    private final PrintStream output;

    public EmailMessageSender() {
        this(true);
    }

    public EmailMessageSender(boolean available) {
        this(available, System.out);
    }

    public EmailMessageSender(boolean available, PrintStream output) {
        this.available = available;
        this.output = Objects.requireNonNull(output, "output must not be null");
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available || !SenderSupport.isValidMessage(recipient, body)) {
            return false;
        }

        output.println("[Email] To: " + recipient);
        output.println("Subject: " + SenderSupport.normalizeTitle(title));
        output.println(body);
        output.println();
        output.println("Regards,");
        output.println("Hotel Guest Services");
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
        return "Email";
    }
}
