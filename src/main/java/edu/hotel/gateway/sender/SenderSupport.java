package edu.hotel.gateway.sender;

/** Small package-private helpers shared by the simulated senders. */
final class SenderSupport {
    private SenderSupport() {
    }

    static boolean isValidMessage(String recipient, String body) {
        return recipient != null && !recipient.isBlank()
                && body != null && !body.isBlank();
    }

    static String normalizeTitle(String title) {
        return title == null ? "" : title;
    }

    /** Escapes the contents of a JSON string; surrounding quotes are added by the caller. */
    static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }
}
