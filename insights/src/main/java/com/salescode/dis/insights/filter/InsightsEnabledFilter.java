package com.salescode.dis.insights.filter;

import com.salescode.dis.insights.service.PropertyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InsightsEnabledFilter extends OncePerRequestFilter {

    @Autowired
    private PropertyService propertyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if(!request.getRequestURI().startsWith("/api/") || !request.getRequestURI().contains("master") || !request.getRequestURI().contains("job")) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI(); // e.g., /api/jobs/run?lob=retail

        // Allow requests to property-related endpoints without filtering
        if (path.startsWith("/api/properties/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract `lob` from header (or query param)
        String lob = request.getHeader("lob");
        if (lob == null) {
            lob = request.getParameter("lob"); // fallback to query param
        }

        if(lob == null) {
            String[] parts = path.split("/");
            if (parts.length >= 3 && parts[1].equals("api")) {
                lob = parts[2];
            }
        }


            if (lob != null) {
            Boolean enabled = propertyService.isInsightsEnabled(lob);

            if (enabled == null) {
                // Try fetching and caching if not present
                try {
                    // Default to 'dev' if env not passed; you may extract env from headers if needed
                    String env = propertyService.getEnvFromLob(lob);
                    String baseUrl = "https://" + env + ".salescode.ai";
                    propertyService.fetchAndCacheFeatureForLob(baseUrl, lob);
                    enabled = propertyService.isInsightsEnabled(lob);
                } catch (Exception e) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Failed to fetch feature property for LOB: " + lob);
                    return;
                }
            }

            if (!enabled) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Insights integration is disabled for LOB: " + lob);
                return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'lob' header or query param");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
