package com.campus.freshman.auth;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminSessions {
    private final Map<String,Instant> sessions=new ConcurrentHashMap<>();
    public String issue() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isBefore(Instant.now()));
        String token=UUID.randomUUID()+"-"+UUID.randomUUID(); sessions.put(token,Instant.now().plusSeconds(8*3600)); return token;
    }
    public boolean valid(String header) {
        if(header==null || !header.startsWith("Bearer ")) return false;
        Instant expiry=sessions.get(header.substring(7)); return expiry!=null && expiry.isAfter(Instant.now());
    }
}
