package com.example.Bill_Generation.Service;

import com.example.Bill_Generation.Configration.AdminConfiguration;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    @Autowired
    SmsService smsService;

    @Autowired
    EmailService emailService;

    @Autowired
    AdminConfiguration adminConfiguration;

    @Autowired
    WhatsAppService whatsAppService;

    public void sendAlert(long productId, String productName, int inventory) {
        String message = String.format("Alert: The inventory for product id '%d' (product name : '%s' ) is below the threshold. Remaining stock: %d", productId, productName, inventory);
        //send alert message
        smsService.sendSms(adminConfiguration.getAdminContactNumber(), message);
        whatsAppService.sendWhatsAppMessage(adminConfiguration.getAdminContactNumber(), message);

        //send email
        try {
            String emailSubject = "Inventory Alert: Low Stock for " + productName;
            String emailTO = adminConfiguration.getAdminEmail();
            String emailFrom = adminConfiguration.getAdminEmail();
            emailService.senEmail(emailTO, emailSubject, message, emailFrom);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
