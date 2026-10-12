package edu.hotel.gateway.sender;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageSenderTest {
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8);

    @Test
    void defaultConstructorsCreateAvailableChannelsWithDistinctNames() {
        MessageSender[] senders = {
                new TelegramMessageSender(), new EmailMessageSender(),
                new SmsMessageSender(), new KafkaEventSender()
        };
        for (MessageSender sender : senders) {
            assertTrue(sender.isAvailable(), sender.getChannelName());
        }
        assertEquals(List.of("Telegram", "Email", "SMS", "Kafka"),
                List.of(senders[0].getChannelName(), senders[1].getChannelName(),
                        senders[2].getChannelName(), senders[3].getChannelName()));
    }

    @Test
    void unavailableChannelsReturnFalseWithoutPrintingASuccess() {
        for (MessageSender sender : senders(false)) {
            assertFalse(sender.isAvailable(), sender.getChannelName());
            assertFalse(sender.sendMessage("guest-101", "Room ready", "Please visit reception."),
                    sender.getChannelName());
        }
        assertEquals("", text());
    }

    @Test
    void availabilityCanBeChangedAtRuntime() {
        TelegramMessageSender telegram = new TelegramMessageSender(false, output);
        EmailMessageSender email = new EmailMessageSender(false, output);
        SmsMessageSender sms = new SmsMessageSender(false, output);
        KafkaEventSender kafka = new KafkaEventSender(false, output);
        MessageSender[] senders = {telegram, email, sms, kafka};

        telegram.setAvailable(true);
        email.setAvailable(true);
        sms.setAvailable(true);
        kafka.setAvailable(true);
        for (MessageSender sender : senders) {
            assertTrue(sender.isAvailable(), sender.getChannelName());
            assertTrue(sender.sendMessage("guest-101", "Room ready", "Please visit reception."),
                    sender.getChannelName());
        }

        telegram.setAvailable(false);
        email.setAvailable(false);
        sms.setAvailable(false);
        kafka.setAvailable(false);
        for (MessageSender sender : senders) {
            assertFalse(sender.isAvailable(), sender.getChannelName());
            assertFalse(sender.sendMessage("guest-101", "Room ready", "Please visit reception."),
                    sender.getChannelName());
        }
    }

    @Test
    void nullAndBlankRecipientIdsAreRejectedByEveryChannel() {
        String[] invalidRecipients = {null, "", "   ", "\t\n"};
        for (MessageSender sender : senders(true)) {
            for (String recipient : invalidRecipients) {
                assertFalse(sender.sendMessage(recipient, "Booking", "Booking confirmed."),
                        sender.getChannelName());
            }
        }
        assertEquals("", text());
    }

    @Test
    void nullAndBlankBodiesAreRejectedByEveryChannel() {
        String[] invalidBodies = {null, "", "   ", "\t\n"};
        for (MessageSender sender : senders(true)) {
            for (String body : invalidBodies) {
                assertFalse(sender.sendMessage("guest-101", "Booking", body), sender.getChannelName());
            }
        }
        assertEquals("", text());
    }

    @Test
    void nullTitleIsAcceptedAndNormalizedToAnEmptyString() {
        for (MessageSender sender : senders(true)) {
            assertTrue(sender.sendMessage("guest-101", null, "Booking confirmed."),
                    sender.getChannelName());
        }
        assertFalse(text().contains("null"));
        assertTrue(text().contains("Subject: \n"));
        assertTrue(text().contains("\"title\":\"\""));
    }

    @Test
    void telegramPrintsABoldTitleAboveTheBody() {
        MessageSender sender = new TelegramMessageSender(true, output);

        assertTrue(sender.sendMessage("guest-101", "Booking confirmed", "Your room is ready."));

        assertEquals("[Telegram] To: guest-101\n**Booking confirmed**\nYour room is ready.\n", text());
    }

    @Test
    void emailAddsASubjectAndTheHotelSignature() {
        MessageSender sender = new EmailMessageSender(true, output);

        assertTrue(sender.sendMessage("guest-101", "Booking confirmed", "Your room is ready."));

        assertEquals("[Email] To: guest-101\nSubject: Booking confirmed\nYour room is ready.\n"
                + "\nRegards,\nHotel Guest Services\n", text());
    }

    @Test
    void smsAccepts160CharactersRejects161AndIgnoresTheTitle() {
        MessageSender sender = new SmsMessageSender(true, output);
        String body = "a".repeat(SmsMessageSender.MAX_LENGTH);

        assertTrue(sender.sendMessage("guest-101", "This title must be ignored", body));
        String successfulOutput = text();
        assertEquals("[SMS] To: guest-101\n" + body + "\n", successfulOutput);

        assertFalse(sender.sendMessage("guest-101", "Ignored", body + "a"));
        assertEquals(successfulOutput, text(), "An oversized message must not be printed or truncated.");
    }

    @Test
    void smsCountsUnicodeCodePointsInsteadOfUtf16CodeUnits() {
        MessageSender sender = new SmsMessageSender(true, output);
        String body = "\uD83D\uDE00".repeat(SmsMessageSender.MAX_LENGTH);

        assertEquals(320, body.length());
        assertEquals(160, body.codePointCount(0, body.length()));
        assertTrue(sender.sendMessage("guest-101", null, body));
        String successfulOutput = text();
        assertFalse(sender.sendMessage("guest-101", null, body + "\uD83D\uDE00"));
        assertEquals(successfulOutput, text());
    }

    @Test
    void kafkaPrintsOneJsonEventAndEscapesSpecialCharactersInEveryField() {
        MessageSender sender = new KafkaEventSender(true, output);

        assertTrue(sender.sendMessage("guest\"42\\desk", "Room \"Ready\"\t",
                "Line 1\nLine 2\r\b\f\t\\" + (char) 1));

        assertEquals("[Kafka] topic=hotel.notifications event="
                + "{\"recipient\":\"guest\\\"42\\\\desk\",\"title\":\"Room \\\"Ready\\\"\\t\","
                + "\"body\":\"Line 1\\nLine 2\\r\\b\\f\\t\\\\\\u0001\"}\n", text());
    }

    private MessageSender[] senders(boolean available) {
        return new MessageSender[]{
                new TelegramMessageSender(available, output),
                new EmailMessageSender(available, output),
                new SmsMessageSender(available, output),
                new KafkaEventSender(available, output)
        };
    }

    private String text() {
        return captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
