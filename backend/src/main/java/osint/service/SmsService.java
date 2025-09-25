package osint.service;

public class SmsService {
    public void sendCode(String phoneNumber, String code) {
        System.out.println("[SMS] To " + phoneNumber + ": code=" + code);
    }
}
