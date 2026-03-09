package com.hashpass.service;

import com.hashpass.model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class UserSession {

    private User user;
    private String encryptionKey;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public boolean isLogged() {
        return this.user != null;
    }

    public void logout() {
        this.user = null;
        this.encryptionKey = null;
    }
}