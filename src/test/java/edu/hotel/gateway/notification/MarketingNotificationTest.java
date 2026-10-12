package edu.hotel.gateway.notification;

import edu.hotel.gateway.sender.EmailMessageSender;
import edu.hotel.gateway.sender.KafkaEventSender;
import edu.hotel.gateway.sender.MessageSender;
import edu.hotel.gateway.sender.SmsMessageSender;
import edu.hotel.gateway.sender.TelegramMessageSender;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketingNotificationTest {
    @Test
    void marketingAddsOfferPrefixTitleAndOptOutText() {
        RecordingSender sender = new RecordingSender("Test channel");
        Notification notification = new MarketingNotification(sender);

        notification.send("guest-101", "Free room upgrade today.");

        assertEquals(1, sender.calls.size());
        RecordingSender.SentMessage call = sender.calls.get(0);
        assertAll(
                () -> assertEquals("guest-101", call.recipient()),
                () -> assertEquals("Hotel offers", call.title()),
                () -> assertEquals("[OFFER] Free room upgrade today.\nTo opt out, contact the front desk.",
                        call.body())
        );
    }

    @Test
    void marketingDoesNotSendWhenItsProviderIsUnavailable() {
        RecordingSender sender = new RecordingSender("Test channel");
        sender.available = false;
        MarketingNotification notification = new MarketingNotification(sender);

        notification.send("guest-101", "Free room upgrade today.");

        assertAll(
                () -> assertEquals(1, sender.availabilityChecks),
                () -> assertTrue(sender.calls.isEmpty())
        );
    }

    @Test
    void newMarketingTypeWorksWithAllFourOriginalProvidersUnchanged() {
        List<Function<PrintStream, MessageSender>> providerFactories = List.of(
                output -> new TelegramMessageSender(true, output),
                output -> new EmailMessageSender(true, output),
                output -> new SmsMessageSender(true, output),
                output -> new KafkaEventSender(true, output)
        );

        for (Function<PrintStream, MessageSender> factory : providerFactories) {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            try (PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
                MessageSender sender = factory.apply(output);
                Notification notification = new MarketingNotification(sender);

                // The complete formatted body is short enough for the SMS limit.
                notification.send("guest-101", "Free room upgrade today.");

                String deliveryOutput = captured.toString(StandardCharsets.UTF_8);
                assertAll(sender.getChannelName(),
                        () -> assertTrue(deliveryOutput.contains("guest-101")),
                        () -> assertTrue(deliveryOutput.contains("[OFFER] Free room upgrade today.")),
                        () -> assertTrue(deliveryOutput.contains("To opt out, contact the front desk."))
                );
            }
        }
    }
}
