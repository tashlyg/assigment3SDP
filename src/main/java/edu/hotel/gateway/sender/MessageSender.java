package edu.hotel.gateway.sender;

/**
 * The implementation side of the Bridge pattern.
 * A notification uses this contract without knowing a channel's details.
 */
public interface MessageSender {
    /** Returns true only when the simulated channel accepts the message. */
    boolean sendMessage(String recipient, String title, String body);

    boolean isAvailable();

    String getChannelName();
}
