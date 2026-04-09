package com.track.subscription_service.auth.dto;

public class GoogleAuthRequest {

    private String credential;

    public String getCredential(){
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
