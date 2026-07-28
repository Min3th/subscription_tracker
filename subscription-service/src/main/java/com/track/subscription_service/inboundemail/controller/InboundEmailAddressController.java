package com.track.subscription_service.inboundemail.controller;

import com.track.subscription_service.inboundemail.dto.InboundEmailAddressResponse;
import com.track.subscription_service.inboundemail.service.InboundEmailAddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inbound-email/address")
public class InboundEmailAddressController {
    private final InboundEmailAddressService addressService;

    public InboundEmailAddressController(InboundEmailAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public InboundEmailAddressResponse getAddress(Authentication authentication) {
        return addressService.getActiveAddress(authentication.getName());
    }

    @PostMapping
    public InboundEmailAddressResponse createAddress(Authentication authentication) {
        return addressService.createIfAbsent(authentication.getName());
    }

    @PostMapping("/rotate")
    public InboundEmailAddressResponse rotateAddress(Authentication authentication) {
        return addressService.rotate(authentication.getName());
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAddress(Authentication authentication) {
        addressService.revoke(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
