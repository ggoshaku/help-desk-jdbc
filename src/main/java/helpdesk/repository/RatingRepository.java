package helpdesk.repository;

import helpdesk.model.AgentRating;
import helpdesk.model.LowRatedTicket;
import helpdesk.model.RatingComment;
import helpdesk.model.TicketRating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository {

    TicketRating save(TicketRating rating);

    Optional<TicketRating> findById(long id);

    Optional<TicketRating> findByTicketId(long ticketId);

    void addComment(long ratingId, long authorId, String text);

    List<RatingComment> findComments(long ratingId);

    List<AgentRating> averageByAgent();

    List<LowRatedTicket> findLowRated(int maxScore);
}
