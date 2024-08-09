package com.example.Bill_Generation.Service;

import com.example.Bill_Generation.Model.*;
import com.example.Bill_Generation.Repository.CustomerRepository;
import com.example.Bill_Generation.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BillService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerRepository customerRepository;

    public Bill generateBill(OrderDetail orderDetail) {
        double totalAmount = 0.0;

        for (OrderItem item : orderDetail.getOrderItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + item.getProductId()));

            double productPrice = product.getPrice();
            double gstRate = product.getGst();
            double itemTotal = item.getQuantity() * productPrice;
            double gstAmount = itemTotal * (gstRate / 100);
            totalAmount += itemTotal + gstAmount;
        }

        Bill bill = new Bill();
        bill.setOrderId(orderDetail.getOrderId());
        bill.setDate(LocalDate.now());
        bill.setTotalAmount(totalAmount);
        bill.setCustomerId(orderDetail.getCustomerId());

        Customer customer = customerRepository.findById(orderDetail.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + orderDetail.getCustomerId()));
        bill.setCustomerName(customer.getName());
        bill.setCustomerEmail(customer.getEmail());
        bill.setCustomerMobileNumber(customer.getMobileNumber());

        return bill;
    }
}
