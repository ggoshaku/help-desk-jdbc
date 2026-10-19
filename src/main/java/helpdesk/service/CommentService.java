package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.exception.UserNotFoundException;
import helpdesk.model.TicketComment;
import helpdesk.model.User;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.TransactionRunner;
import helpdesk.repository.UserRepository;

import java.util.List;

public class CommentService {

    private final TransactionRunner transactions;
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CommentService(TransactionRunner transactions,
                          CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.transactions = transactions;
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public TicketComment addComment(long ticketId, long authorId, String text, boolean internal) {
        ticketRepository.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        User author = userRepository.findById(authorId).orElseThrow(() -> new UserNotFoundException(authorId));
        if (internal && !author.canWriteInternalComments()) {
            throw new AccessDeniedException(author.getName() + " не может оставлять внутренние комментарии");
        }
        return transactions.inTransaction(() -> {
            TicketComment comment = commentRepository.add(new TicketComment(ticketId, author, text, internal));
            auditService.commentAdded(authorId, comment.getId(), ticketId);
            return comment;
        });
    }

    public List<TicketComment> findAll(long ticketId) {
        return commentRepository.findByTicketId(ticketId);
    }

    public List<TicketComment> findVisibleToCustomer(long ticketId) {
        return commentRepository.findExternalByTicketId(ticketId);
    }
}
