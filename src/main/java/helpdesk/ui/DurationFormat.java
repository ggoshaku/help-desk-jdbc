package helpdesk.ui;

import java.time.Duration;

public final class DurationFormat {

    private DurationFormat() {
    }

    public static String format(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours >= 24) {
            return hours / 24 + " д " + hours % 24 + " ч";
        }
        return hours > 0 ? hours + " ч " + minutes + " мин" : minutes + " мин";
    }
}
