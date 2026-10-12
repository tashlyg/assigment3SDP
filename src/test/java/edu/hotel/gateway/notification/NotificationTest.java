package edu.hotel.gateway.notification;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTest {
    @Test
    void urgentAddsPrefixAndStopsAfterPrimarySuccess() {
        RecordingSender primary = new RecordingSender("Primary");
        RecordingSender fallback = new RecordingSender("Fallback");
        Notification notification = new UrgentNotification(primary, fallback);

        notification.send("guest-101", "Please leave the lobby.");

        assertAll(
                () -> assertEquals(1, primary.calls.size()),
                () -> assertEquals("guest-101", primary.calls.get(0).recipient()),
                () -> assertEquals("[URGENT] Please leave the lobby.", primary.calls.get(0).body()),
                () -> assertTrue(fallback.calls.isEmpty()),
                () -> assertEquals(0, fallback.availabilityChecks)
        );
    }

    @Test
    void urgentUsesFallbackWhenPrimaryIsUnavailable() {
        RecordingSender primary = new RecordingSender("Primary");
        primary.available = false;
        RecordingSender fallback = new RecordingSender("Fallback");
        UrgentNotification notification = new UrgentNotification(primary, fallback);

        notification.send("guest-101", "Fire drill at 10:00.");

        assertAll(
                () -> assertTrue(primary.calls.isEmpty()),
                () -> assertEquals(1, fallback.calls.size()),
                () -> assertEquals("[URGENT] Fire drill at 10:00.", fallback.calls.get(0).body())
        );
    }

    @Test
    void urgentUsesFallbackWhenAvailablePrimaryRejectsDelivery() {
        RecordingSender primary = new RecordingSender("Primary");
        primary.deliverySucceeds = false;
        RecordingSender fallback = new RecordingSender("Fallback");
        UrgentNotification notification = new UrgentNotification(primary, fallback);

        notification.send("guest-101", "Reception has moved.");

        assertAll(
                () -> assertEquals(1, primary.calls.size()),
                () -> assertEquals(1, fallback.calls.size()),
                () -> assertEquals(primary.calls.get(0).body(), fallback.calls.get(0).body())
        );
    }

    @Test
    void urgentDoesNotSendWhenBothProvidersAreUnavailable() {
        RecordingSender primary = new RecordingSender("Primary");
        primary.available = false;
        RecordingSender fallback = new RecordingSender("Fallback");
        fallback.available = false;
        UrgentNotification notification = new UrgentNotification(primary, fallback);

        notification.send("guest-101", "Reception has moved.");

        assertAll(
                () -> assertTrue(primary.calls.isEmpty()),
                () -> assertTrue(fallback.calls.isEmpty()),
                () -> assertEquals(1, primary.availabilityChecks),
                () -> assertEquals(1, fallback.availabilityChecks)
        );
    }

    @Test
    void urgentCanUseAnAdditionalFallbackAddedAtRuntime() {
        RecordingSender primary = new RecordingSender("Primary");
        primary.available = false;
        RecordingSender firstFallback = new RecordingSender("First fallback");
        firstFallback.deliverySucceeds = false;
        RecordingSender secondFallback = new RecordingSender("Second fallback");
        UrgentNotification notification = new UrgentNotification(primary, firstFallback);
        notification.addFallbackSender(secondFallback);

        notification.send("guest-101", "Reception has moved.");

        assertAll(
                () -> assertTrue(primary.calls.isEmpty()),
                () -> assertEquals(1, firstFallback.calls.size()),
                () -> assertEquals(1, secondFallback.calls.size())
        );
    }

    @Test
    void encryptedSendsExactUtf8Base64AndPreservesOriginalContentAfterDecoding() {
        RecordingSender sender = new RecordingSender("Test channel");
        EncryptedNotification notification = new EncryptedNotification(sender);
        String message = "Бронирование подтверждено ✅";

        notification.send("guest-101", message);

        assertEquals(1, sender.calls.size());
        String sentBody = sender.calls.get(0).body();
        assertAll(
                () -> assertEquals(Base64.getEncoder().encodeToString(
                        message.getBytes(StandardCharsets.UTF_8)), sentBody),
                () -> assertEquals(message, new String(
                        Base64.getDecoder().decode(sentBody), StandardCharsets.UTF_8))
        );
    }

    @Test
    void batchCountsActualResultsAndContinuesAfterAnIndividualFailure() {
        RecordingSender sender = new RecordingSender("Test channel");
        sender.rejectedRecipients.add("guest-102");
        ScheduledBatchNotification notification = new ScheduledBatchNotification(
                sender, List.of("guest-101", "guest-102", "guest-103"));

        notification.send("Breakfast starts at 07:00.");

        assertAll(
                () -> assertEquals(new BatchSummary(3, 2, 1), notification.getLastSummary()),
                () -> assertEquals(List.of("guest-101", "guest-102", "guest-103"),
                        sender.calls.stream().map(RecordingSender.SentMessage::recipient).toList()),
                () -> assertTrue(sender.calls.stream()
                        .allMatch(call -> call.body().equals("Breakfast starts at 07:00.")))
        );
    }

    @Test
    void batchWithNoRecipientsProducesZeroSummaryAndDoesNotSend() {
        RecordingSender sender = new RecordingSender("Test channel");
        ScheduledBatchNotification notification = new ScheduledBatchNotification(sender, List.of());

        notification.send("Breakfast starts at 07:00.");

        assertAll(
                () -> assertEquals(new BatchSummary(0, 0, 0), notification.getLastSummary()),
                () -> assertTrue(sender.calls.isEmpty())
        );
    }

    @Test
    void batchDefensivelyCopiesTheRecipientCollection() {
        RecordingSender sender = new RecordingSender("Test channel");
        List<String> sourceRecipients = new ArrayList<>(List.of("guest-101"));
        ScheduledBatchNotification notification = new ScheduledBatchNotification(sender, sourceRecipients);
        sourceRecipients.clear();
        sourceRecipients.add("guest-999");

        notification.send("Breakfast starts at 07:00.");

        assertAll(
                () -> assertEquals(new BatchSummary(1, 1, 0), notification.getLastSummary()),
                () -> assertEquals("guest-101", sender.calls.get(0).recipient())
        );
    }

    @Test
    void batchTwoArgumentOverloadUsesTheExplicitRecipient() {
        RecordingSender sender = new RecordingSender("Test channel");
        ScheduledBatchNotification notification = new ScheduledBatchNotification(
                sender, List.of("guest-101", "guest-102"));

        notification.send("guest-999", "Please visit reception.");

        assertAll(
                () -> assertEquals(new BatchSummary(1, 1, 0), notification.getLastSummary()),
                () -> assertEquals(1, sender.calls.size()),
                () -> assertEquals("guest-999", sender.calls.get(0).recipient())
        );
    }

    @Test
    void sameNotificationObjectCanSwitchItsSenderAtRuntime() {
        RecordingSender first = new RecordingSender("First channel");
        RecordingSender second = new RecordingSender("Second channel");
        Notification notification = new UrgentNotification(first);

        notification.send("guest-101", "First message");
        notification.setSender(second);
        notification.send("guest-101", "Second message");

        assertAll(
                () -> assertEquals(1, first.calls.size()),
                () -> assertEquals("[URGENT] First message", first.calls.get(0).body()),
                () -> assertEquals(1, second.calls.size()),
                () -> assertEquals("[URGENT] Second message", second.calls.get(0).body())
        );
    }

    @Test
    void nullSenderIsRejectedWithoutReplacingTheWorkingSender() {
        RecordingSender sender = new RecordingSender("Working channel");
        Notification notification = new UrgentNotification(sender);

        assertThrows(NullPointerException.class, () -> new UrgentNotification(null));
        assertThrows(NullPointerException.class, () -> notification.setSender(null));
        notification.send("guest-101", "Still works.");

        assertEquals(1, sender.calls.size());
    }
}
