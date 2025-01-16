package com.applicate.services.channelkart.scanner;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalRegistryScannerAdapter implements ExternalRegistryScannerInterface{

    public List<BundleResource> getResources(){
        return ExternalRegistryScanner.getInstance().getResources();
    }
}
