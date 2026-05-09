package com.jay.LetsSplitIt.Events;

import com.jay.LetsSplitIt.Dto.SettleResponse;
import com.jay.LetsSplitIt.Dto.SettlementCreatedEvent;
import com.jay.LetsSplitIt.Entities.Group;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.GroupRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class SettlementCreatedMailListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementCreatedMailListener.class);

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MailService mailService;

    SettlementCreatedMailListener(UserRepository userRepository,
                                  GroupRepository groupRepository,
                                  MailService mailService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.mailService = mailService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettlementCreated(SettlementCreatedEvent event) {
        SettleResponse settlement = event.settlement();

        Optional<User> payerOpt = userRepository.findById(settlement.payerId());
        Optional<User> receiverOpt = userRepository.findById(settlement.receiverId());
        if (payerOpt.isEmpty() || receiverOpt.isEmpty()) {
            log.warn("Skipping settlement mail; payer={} receiver={} (one not found)",
                    settlement.payerId(), settlement.receiverId());
            return;
        }

        User payer = payerOpt.get();
        User receiver = receiverOpt.get();
        String contextLine = scopeLine(settlement);

        sendToReceiver(receiver, payer, settlement.amountSettled(), contextLine);
//        sendToPayer(payer, receiver, settlement.amountSettled(), contextLine);
    }

    private void sendToReceiver(User receiver, User payer, BigDecimal amount, String contextLine) {
        String subject = payer.getName() + " settled " + amount + " with you";
        String body = "Hi " + receiver.getName() + ",\n\n"
                + payer.getName() + " marked a settlement of " + amount + " to you on LetsSplitIt.\n"
                + contextLine
                + "\nOpen the app to review the updated balance.\n\n"
                + "— LetsSplitIt";
        mailService.sendSimpleMail(receiver.getEmail(), subject, body);
    }

//    private void sendToPayer(User payer, User receiver, BigDecimal amount, String contextLine) {
//        String subject = "Settlement recorded: " + amount + " to " + receiver.getName();
//        String body = "Hi " + payer.getName() + ",\n\n"
//                + "We recorded your settlement of " + amount + " to " + receiver.getName() + ".\n"
//                + contextLine
//                + "\n— LetsSplitIt";
//        mailService.sendSimpleMail(payer.getEmail(), subject, body);
//    }

    private String scopeLine(SettleResponse settlement) {
        if (settlement.scope() == SettleResponse.Scope.GROUP && settlement.groupId() != null) {
            String groupName = groupRepository.findById(settlement.groupId())
                    .map(Group::getName)
                    .orElse(settlement.groupId().toString());
            return "Context: group \"" + groupName + "\".\n";
        }
        if (settlement.scope() == SettleResponse.Scope.FULL) {
            return "Context: full settle-up across all balances between you.\n";
        }
        return "Context: direct (non-group) balance.\n";
    }
}