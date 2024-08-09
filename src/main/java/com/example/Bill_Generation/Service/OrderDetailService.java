package com.example.Bill_Generation.Service;

import com.example.Bill_Generation.Model.*;
import com.example.Bill_Generation.Repository.*;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

@Service
public class OrderDetailService {

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    OrderDetailRepository orderDetailRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    BillRepository billRepository;

    @Autowired
    SmsService smsService;

    @Autowired
    WhatsAppService whatsAppMessage;

    @Autowired
    BillService billService;

    @Autowired
    AlertService alertService;

    private static final int INVENTORY_THRESHOLD = 10;

    public ResponseDTO<OrderDetail> placeOrder(OrderDetail orderDetail) {
        try {
            for (OrderItem orderItem : orderDetail.getOrderItems()) {
                Product product = productRepository.findById(orderItem.getProductId()).orElseThrow(() -> new RuntimeException("Product not found with ID: " + orderItem.getProductId()));
                if (product.getInventory() < orderItem.getQuantity()) {
                    return new ResponseDTO<>(null, HttpStatus.BAD_REQUEST, "Insufficient inventory for product ID: " + orderItem.getProductId());
                }
            }

            boolean paymentSuccess = processPayment();
            if (!paymentSuccess) {
                return new ResponseDTO<>(null, HttpStatus.PAYMENT_REQUIRED, "payment failed");
            }

            OrderDetail savedOrderDetail = orderDetailRepository.save(orderDetail);
            orderItemRepository.saveAll(orderDetail.getOrderItems());

            Bill bill = billService.generateBill(savedOrderDetail);
            billRepository.save(bill);

            Customer customer = customerRepository.findById(orderDetail.getCustomerId()).orElseThrow(() -> new RuntimeException("Product not found with ID: " + orderDetail.getCustomerId()));
            String customerPhoneNumber = String.valueOf(customer.getMobileNumber());

            String message = String.format(
                    "Hello %s! Your order with ID %s has been successfully placed. Your total payment is %.2f. Thank you for choosing us. We'll notify you once it's on its way.",
                    customer.getName(),
                    orderDetail.getOrderId(),
                    bill.getTotalAmount()
            );
            smsService.sendSms(customerPhoneNumber, message);
            whatsAppMessage.sendWhatsAppMessage(customerPhoneNumber, message);

            for (OrderItem orderItem : savedOrderDetail.getOrderItems()) {
                Product product = productRepository.findById(orderItem.getProductId()).orElseThrow(() -> new RuntimeException("Product not found with ID: " + orderItem.getProductId()));
                product.setInventory(product.getInventory() - orderItem.getQuantity());
                productRepository.save(product);

                if (product.getInventory() < INVENTORY_THRESHOLD) {
                    alertService.sendAlert(product.getProductId(), product.getProductName(), product.getInventory());
                }
            }

            return new ResponseDTO<>(savedOrderDetail, HttpStatus.OK, "Order placed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "failed to place");
        }
    }

    private boolean processPayment() {
        Random random = new Random();
        return random.nextInt(4) != 0;
    }

}
