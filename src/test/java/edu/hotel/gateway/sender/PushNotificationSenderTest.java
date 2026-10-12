package edu.hotel.gateway.sender;

import edu.hotel.gateway.notification.BatchSummary;
import edu.hotel.gateway.notification.EncryptedNotification;
import edu.hotel.gateway.notification.Notification;
import edu.hotel.gateway.notification.ScheduledBatchNotification;
import edu.hotel.gateway.notification.UrgentNotification;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Extension tests are separate so the original test suite remains unchanged. */
class PushNotificationSenderTest {
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8);

    @Test
    void pushPrintsTheRecipientTitleAndBody() {
        MessageSender sender = new PushNotificationSender(true, output);

        assertEquals("Push", sender.getChannelName());
        assertTrue(sender.sendMessage("guest-101", "Room ready", "Please collect your key."));
        assertEquals("[Push] To: guest-101\nTitle: Room ready\nPlease collect your key.\n", text());
    }

    @Test
    void pushAvailabilityCanBeConfiguredAndChangedAtRuntime() {
        assertTrue(new PushNotificationSender().isAvailable());
        assertFalse(new PushNotificationSender(false).isAvailable());
        PushNotificationSender sender = new PushNotificationSender(false, output);

        assertFalse(sender.sendMessage("guest-101", "Room ready", "Please collect your key."));
        assertEquals("", text());

        sender.setAvailable(true);
        assertTrue(sender.isAvailable());
        assertTrue(sender.sendMessage("guest-101", "Room ready", "Please collect your key."));
        String successfulOutput = text();

        sender.setAvailable(false);
        assertFalse(sender.isAvailable());
        assertFalse(sender.sendMessage("guest-101", "Room ready", "Please collect your key."));
        assertEquals(successfulOutput, text());
    }

    @Test
    void pushHonorsTheExistingValidationAndOptionalTitleContract() {
        MessageSender sender = new PushNotificationSender(true, output);

        assertFalse(sender.sendMessage(null, "Title", "Body"));
        assertFalse(sender.sendMessage("  ", "Title", "Body"));
        assertFalse(sender.sendMessage("guest-101", "Title", null));
        assertFalse(sender.sendMessage("guest-101", "Title", "\t\n"));
        assertEquals("", text());

        assertTrue(sender.sendMessage("guest-101", null, "Body"));
        assertEquals("[Push] To: guest-101\nTitle: \nBody\n", text());
    }

    @Test
    void allThreeUnchangedNotificationClassesCanUsePushThroughTheBridge() {
        MessageSender push = new PushNotificationSender(true, output);

        Notification urgent = new UrgentNotification(push);
        urgent.send("guest-101", "Check reception.");
        assertEquals("[Push] To: guest-101\nTitle: Hotel alert\n[URGENT] Check reception.\n", text());

        captured.reset();
        Notification encrypted = new EncryptedNotification(push);
        encrypted.send("guest-102", "Hotel");
        assertEquals("[Push] To: guest-102\nTitle: Encoded hotel message\nSG90ZWw=\n", text());

        captured.reset();
        ScheduledBatchNotification batch = new ScheduledBatchNotification(
                push, List.of("guest-103", "guest-104"));
        batch.send("Check-out at 12:00.");
        assertEquals(new BatchSummary(2, 2, 0), batch.getLastSummary());
        assertEquals("[Push] To: guest-103\nTitle: Hotel guest update\nCheck-out at 12:00.\n"
                + "[Push] To: guest-104\nTitle: Hotel guest update\nCheck-out at 12:00.\n", text());
    }

    private String text() {
        return captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
