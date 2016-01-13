package be.maartenvg.core.logging;

import java.time.LocalDateTime;

public class LogItem {
    private final LogAction logAction;
    private final LocalDateTime timestamp;
    private final String description;

    public LogItem(LogAction logAction) {
        this.logAction = logAction;
        this.timestamp = LocalDateTime.now();
        this.description = null;
    }

    public LogItem(LogAction logAction, String description) {
        this.logAction = logAction;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public String getLogAction() {
        return logAction.name();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }
}
