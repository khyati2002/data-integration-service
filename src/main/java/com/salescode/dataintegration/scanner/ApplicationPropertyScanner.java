package com.salescode.dataintegration.scanner;

import com.salescode.channelkart.utils.SecurityContextUtils;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@ConfigurationProperties(prefix = "app")
public class ApplicationPropertyScanner {

    private String lob;

    public void setLob(String appLob) {
        lob=appLob;
        SecurityContextUtils.setLob(lob);
    }
}
