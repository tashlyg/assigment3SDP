package edu.hotel.gateway;

import edu.hotel.gateway.notification.EncryptedNotification;
import edu.hotel.gateway.notification.MarketingNotification;
import edu.hotel.gateway.notification.Notification;
import edu.hotel.gateway.notification.ScheduledBatchNotification;
import edu.hotel.gateway.notification.UrgentNotification;
import edu.hotel.gateway.sender.EmailMessageSender;
import edu.hotel.gateway.sender.KafkaEventSender;
import edu.hotel.gateway.sender.PushNotificationSender;
import edu.hotel.gateway.sender.SmsMessageSender;
import edu.hotel.gateway.sender.TelegramMessageSender;

import java.util.List;

/** Composition root: chooses which notification object uses which sender. */
public class Main {
    public static void main(String[] args) {
        System.out.println("HOTEL NOTIFICATION GATEWAY | BRIDGE PATTERN");
        System.out.println("All deliveries are simulated. Recipient strings are logical guest IDs.");

        TelegramMessageSender telegram = new TelegramMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        KafkaEventSender kafka = new KafkaEventSender();

        section("1. UrgentNotification + Telegram");
        new UrgentNotification(telegram, sms)
                .send("guest-101", "Water maintenance starts in 15 minutes.");

        section("2. EncryptedNotification + Email");
        System.out.println("Educational Base64 encoding only; not secure encryption.");
        new EncryptedNotification(email)
                .send("guest-102", "Demo booking DEMO-BK-1042: room 204 confirmed.");

        section("3. ScheduledBatchNotification + SMS");
        System.out.println("The caller starts the batch now; two blank IDs demonstrate failed delivery.");
        List<String> guests = List.of(
                "guest-101", "guest-102", "guest-103", "guest-104",
                "guest-105", "guest-106", "guest-107", "guest-108", "", " ");
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms, guests);
        batch.send("Reminder: hotel checkout is at 12:00. Thank you for staying with us.");

        section("4. Runtime switching: Telegram -> Email");
        Notification notification = new UrgentNotification(telegram, sms);
        notification.send("guest-103", "Please contact the front desk.");
        // Same object. Only its MessageSender reference changes.
        notification.setSender(email);
        notification.send("guest-103", "Please contact the front desk.");

        section("5. Telegram unavailable -> fallback to SMS");
        telegram.setAvailable(false);
        new UrgentNotification(telegram, sms)
                .send("guest-104", "Your airport transfer has arrived.");
        telegram.setAvailable(true);

        section("6. MarketingNotification + PushNotificationSender");
        new MarketingNotification(new PushNotificationSender())
                .send("guest-107", "Book your next stay with code HOTEL10 for a 10% discount.");

        section("7. Kafka simulated event publishing");
        new UrgentNotification(kafka)
                .send("front-desk", "Guest requested a late checkout.");

        section("8. SMS rejects a message over its limit");
        boolean sent = sms.sendMessage("guest-105", "Ignored SMS title",
                "A".repeat(SmsMessageSender.MAX_LENGTH + 1));
        System.out.println("Delivery result: " + sent + " (expected false)");

        section("9. Primary and fallback are both unavailable");
        new UrgentNotification(new TelegramMessageSender(false), new SmsMessageSender(false))
                .send("guest-106", "Please contact the front desk.");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
