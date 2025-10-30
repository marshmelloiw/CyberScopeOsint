package osint.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class ApiKeyService {
    private final AtomicReference<String> shodanKey = new AtomicReference<>(System.getenv("SHODAN_API_KEY"));
    private final AtomicReference<String> vtKey = new AtomicReference<>(System.getenv("VT_API_KEY"));
    private final AtomicReference<String> hibpKey = new AtomicReference<>(System.getenv("HIBP_API_KEY"));

    public String getShodanKey() { return nullToEmpty(shodanKey.get()); }
    public void setShodanKey(String v) { shodanKey.set(v); }

    public String getVirusTotalKey() { return nullToEmpty(vtKey.get()); }
    public void setVirusTotalKey(String v) { vtKey.set(v); }

    public String getHibpKey() { return nullToEmpty(hibpKey.get()); }
    public void setHibpKey(String v) { hibpKey.set(v); }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}


