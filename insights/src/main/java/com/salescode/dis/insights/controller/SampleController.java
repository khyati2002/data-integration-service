    package com.salescode.dis.insights.controller;

    import com.salescode.auth.sdk.filters.models.AuthUser;
    import com.salescode.auth.sdk.filters.utils.SecurityUtils;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    @RequestMapping("/api")
    public class SampleController {

        @GetMapping("/userinfo")
        public AuthUser getUserInfo() {
            // Retrieves the active authenticated user or fails if not authenticated
            return SecurityUtils.getActiveUserOrFail();
        }

        @GetMapping("/status")
        public String getServiceStatus() {
            // Simple health check or status endpoint
            return "Service is running";
        }
    }