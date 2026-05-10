package com.jay.LetsSplitIt.Events;

import com.jay.LetsSplitIt.Dto.ExpenseCreatedEvent;
import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class ExpenseCreatedMailListener {

    private static final Logger log = LoggerFactory.getLogger(ExpenseCreatedMailListener.class);

    private final UserRepository userRepository;
    private final MailService mailService;

    ExpenseCreatedMailListener(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        Optional<User> payerOpt = userRepository.findById(event.paidBy());
        if (payerOpt.isEmpty()) {
            log.warn("Payer {} not found; skipping debtor notifications for expense {}",
                    event.paidBy(), event.expenseId());
            return;
        }
        User payer = payerOpt.get();

        for (ExpenseShare share : event.shares()) {
            if (share.userId().equals(event.paidBy())) {
                continue;
            }
            userRepository.findById(share.userId()).ifPresentOrElse(
                    debtor -> sendMail(debtor, payer, share, event),
                    () -> log.warn("Debtor {} not found; skipping mail for expense {}",
                            share.userId(), event.expenseId())
            );
        }
    }

    private void sendMail(User debtor, User payer, ExpenseShare share, ExpenseCreatedEvent event) {
        String subject = "New expense \"" + event.title() + "\": you owe " + share.amountOwed() + " to " + payer.getName();
        StringBuilder body = new StringBuilder()
                .append("Hi ").append(debtor.getName()).append(",\n\n")
                .append(payer.getName()).append(" added a new expense on LetsSplitIt.\n\n")
                .append("Title: ").append(event.title()).append("\n")
                .append("Category: ").append(event.category()).append("\n");
        if (event.description() != null && !event.description().isBlank()) {
            body.append("Description: ").append(event.description()).append("\n");
        }
        body.append("Total amount: ").append(event.totalAmount()).append("\n")
                .append("Split type: ").append(event.splitType()).append("\n")
                .append("Your share: ").append(share.amountOwed()).append("\n\n")
                .append("Open the app to review and settle up.\n\n")
                .append("— LetsSplitIt");
        mailService.sendSimpleMail(debtor.getEmail(), subject, body.toString());
    }
}