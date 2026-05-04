package com.udea.lab1As.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.lab1As.dto.TransactionDto;
import com.udea.lab1As.entity.Customer;
import com.udea.lab1As.entity.Transaction;
import com.udea.lab1As.exception.CustomerNotFoundException;
import com.udea.lab1As.exception.InsufficientFundsException;
import com.udea.lab1As.exception.InvalidTransactionException;
import com.udea.lab1As.mapper.TransactionMapper;
import com.udea.lab1As.repository.CustomerRepository;
import com.udea.lab1As.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository,
            CustomerRepository customerRepository,
            TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional // Si algo falla, todo se revierte (rollback)
    public TransactionDto transferMoney(TransactionDto transactionDto) {
        validateTransferRequest(transactionDto);

        var sender = getCustomerByAccount(transactionDto.getSenderAccountNumber(), "Sender account not found");
        var receiver = getCustomerByAccount(transactionDto.getReceiverAccountNumber(), "Receiver account not found");
        validateSufficientFunds(sender, transactionDto.getAmount());

        applyTransfer(sender, receiver, transactionDto.getAmount());
        customerRepository.save(sender);
        customerRepository.save(receiver);

        var transaction = transactionMapper.toEntity(transactionDto);
        transaction.setSenderAccountNumber(sender.getAccountNumber());
        transaction.setReceiverAccountNumber(receiver.getAccountNumber());

        var savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    // obtener la lista de todas las transacciones por numero de cuenta
    public List<TransactionDto> getTransactionsByAccountNumber(String accountNumber) {
        List<Transaction> transactions = transactionRepository
                .findBySenderAccountNumberOrReceiverAccountNumber(accountNumber, accountNumber);
        return transactions.stream().map(transactionMapper::toDto).toList();
    }

    private void validateTransferRequest(TransactionDto transactionDto) {
        if (transactionDto.getSenderAccountNumber() == null || transactionDto.getReceiverAccountNumber() == null) {
            throw new InvalidTransactionException("Account numbers cannot be void");
        }
        if (transactionDto.getSenderAccountNumber().equals(transactionDto.getReceiverAccountNumber())) {
            throw new InvalidTransactionException("Sender and receiver accounts must be different");
        }
        if (transactionDto.getAmount() == null || transactionDto.getAmount() <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than zero");
        }
        if (transactionDto.getTransactionDate() == null) {
            throw new InvalidTransactionException("Transaction date is required");
        }
    }

    private Customer getCustomerByAccount(String accountNumber, String notFoundMessage) {
        return customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomerNotFoundException(notFoundMessage));
    }

    private void validateSufficientFunds(Customer sender, Double amount) {
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
    }

    private void applyTransfer(Customer sender, Customer receiver, Double amount) {
        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);
    }

}
