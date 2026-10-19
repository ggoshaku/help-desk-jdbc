package helpdesk.model;

import org.junit.jupiter.api.Test;

import static helpdesk.model.TicketStatus.CANCELLED;
import static helpdesk.model.TicketStatus.CLOSED;
import static helpdesk.model.TicketStatus.IN_PROGRESS;
import static helpdesk.model.TicketStatus.NEW;
import static helpdesk.model.TicketStatus.RESOLVED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketStatusTest {

    @Test
    void lifecycleTransitionsAreAllowed() {
        assertTrue(NEW.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(RESOLVED));
        assertTrue(RESOLVED.canTransitionTo(CLOSED));
        assertTrue(RESOLVED.canTransitionTo(IN_PROGRESS));
    }

    @Test
    void activeTicketCanBeCancelled() {
        assertTrue(NEW.canTransitionTo(CANCELLED));
        assertTrue(IN_PROGRESS.canTransitionTo(CANCELLED));
    }

    @Test
    void closedAndCancelledAreFinal() {
        for (TicketStatus status : TicketStatus.values()) {
            assertFalse(CLOSED.canTransitionTo(status));
            assertFalse(CANCELLED.canTransitionTo(status));
        }
    }

    @Test
    void skippingStepsIsForbidden() {
        assertFalse(NEW.canTransitionTo(RESOLVED));
        assertFalse(NEW.canTransitionTo(CLOSED));
        assertFalse(IN_PROGRESS.canTransitionTo(CLOSED));
    }
}
