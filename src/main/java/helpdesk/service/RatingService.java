package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.RatingException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.exception.UserNotFoundException;
import helpdesk.model.Administrator;
import helpdesk.model.AgentRating;
import helpdesk.model.AuditAction;
import helpdesk.model.LowRatedTicket;
import helpdesk.model.RatingComment;
import helpdesk.model.Ticket;
import helpdesk.model.TicketRating;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.RatingRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.TransactionRunner;
import helpdesk.repository.UserRepository;

import java.util.List;

public class RatingService {

    private final TransactionRunner transactions;
    private final RatingRepository ratingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RatingService(TransactionRunner transactions,
                         RatingRepository ratingRepository,
                         TicketRepository ticketRepository,
                         UserRepository userRepository,
                         AuditService auditService) {
        this.transactions = transactions;
        this.ratingRepository = ratingRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public TicketRating rateTicket(long ticketId, long customerId, int score, String comment) {
        return transactions.inTransaction(() -> {
            Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));
            if (ticket.getStatus() != TicketStatus.CLOSED) {
                throw new RatingException("Оценить можно только закрытую заявку, а №" + ticketId
                        + " в статусе " + ticket.getStatus());
            }
            if (ticket.getCustomerId() != customerId) {
                throw new AccessDeniedException("Оценить заявку №" + ticketId + " может только её автор");
            }
            if (ticket.getAssigneeId() == null) {
                throw new RatingException("У заявки №" + ticketId + " не было исполнителя");
            }
            if (ratingRepository.findByTicketId(ticketId).isPresent()) {
                throw new RatingException("Заявка №" + ticketId + " уже оценена");
            }

            TicketRating rating = ratingRepository.save(
                    new TicketRating(ticketId, customerId, ticket.getAssigneeId(), score));
            if (comment != null && !comment.isBlank()) {
                ratingRepository.addComment(rating.getId(), customerId, comment.strip());
            }
            auditService.ticketAction(customerId, AuditAction.RATE_TICKET, ticketId, "оценка " + score);
            return rating;
        });
    }

    public void replyToRating(long ratingId, long authorId, String text) {
        TicketRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingException("Оценка №" + ratingId + " не найдена"));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new UserNotFoundException(authorId));
        if (rating.getAgentId() != authorId && !(author instanceof Administrator)) {
            throw new AccessDeniedException(author.getName() + " не может отвечать на эту оценку");
        }
        ratingRepository.addComment(ratingId, authorId, text);
    }

    public List<RatingComment> comments(long ratingId) {
        return ratingRepository.findComments(ratingId);
    }

    public List<AgentRating> averageByAgent() {
        return ratingRepository.averageByAgent();
    }

    public List<LowRatedTicket> findLowRated(int maxScore) {
        return ratingRepository.findLowRated(maxScore);
    }
}
