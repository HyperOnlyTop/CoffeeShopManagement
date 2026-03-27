package com.example.QuanLyQuanCafe.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }
}
