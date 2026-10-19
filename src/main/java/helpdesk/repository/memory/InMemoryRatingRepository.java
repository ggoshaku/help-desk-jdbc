package helpdesk.repository.memory;

import helpdesk.model.AgentRating;
import helpdesk.model.LowRatedTicket;
import helpdesk.model.RatingComment;
import helpdesk.model.TicketRating;
import helpdesk.repository.RatingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryRatingRepository implements RatingRepository {
    private final List<TicketRating> ratings = new ArrayList<>();
    private final List<RatingComment> comments = new ArrayList<>();

    @Override
    public TicketRating save(TicketRating rating) {
        rating.markSaved(ratings.size() + 1L, LocalDateTime.now());
        ratings.add(rating);
        return rating;
    }

    @Override
    public Optional<TicketRating> findById(long id) {
        return ratings.stream().filter(rating -> rating.getId() == id).findFirst();
    }

    @Override
    public Optional<TicketRating> findByTicketId(long ticketId) {
        return ratings.stream().filter(rating -> rating.getTicketId() == ticketId).findFirst();
    }

    @Override
    public void addComment(long ratingId, long authorId, String text) {
        comments.add(new RatingComment(comments.size() + 1L, ratingId, authorId, null, text, LocalDateTime.now()));
    }

    @Override
    public List<RatingComment> findComments(long ratingId) {
        return comments.stream().filter(comment -> comment.ratingId() == ratingId).toList();
    }

    @Override
    public List<AgentRating> averageByAgent() {
        return List.of();
    }

    @Override
    public List<LowRatedTicket> findLowRated(int maxScore) {
        return List.of();
    }
}
