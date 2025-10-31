package osint.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {
    public void sendCode(String phoneNumber, String code) {
        System.out.println("[SMS] To " + phoneNumber + ": code=" + code);
    }

    public void sendSms(String phoneNumber, String message) {
        System.out.println("[SMS] To " + phoneNumber + ": message=" + message);
    }
}
