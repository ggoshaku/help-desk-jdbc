package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record RatingComment(long id, long ratingId, long authorId, String authorName, String text,
                            LocalDateTime createdAt) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    public String toString() {
        return createdAt.format(FORMAT) + " " + authorName + ": " + text;
    }
}
