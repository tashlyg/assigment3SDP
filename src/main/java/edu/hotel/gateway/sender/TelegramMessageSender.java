package edu.hotel.gateway.sender;

import java.io.PrintStream;
import java.util.Objects;

/** A simulated Telegram channel with a bold title and message body. */
public final class TelegramMessageSender implements MessageSender {
    private boolean available;
    private final PrintStream output;

    public TelegramMessageSender() {
        this(true);
    }

    public TelegramMessageSender(boolean available) {
        this(available, System.out);
    }

    public TelegramMessageSender(boolean available, PrintStream output) {
        this.available = available;
        this.output = Objects.requireNonNull(output, "output must not be null");
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available || !SenderSupport.isValidMessage(recipient, body)) {
            return false;
        }

        String normalizedTitle = SenderSupport.normalizeTitle(title);
        output.println("[Telegram] To: " + recipient);
        if (!normalizedTitle.isEmpty()) {
            output.println("**" + normalizedTitle + "**");
        }
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
        return "Telegram";
    }
}
