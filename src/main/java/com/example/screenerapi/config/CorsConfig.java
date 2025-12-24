package com.example.screenerapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration
 * 
 * WHY THIS FILE IS NECESSARY:
 * ============================
 * This configuration is essential for enabling communication between the React frontend
 * (running on http://localhost:3000) and the Spring Boot backend API (running on 
 * http://localhost:8080). 
 * 
 * By default, web browsers enforce the Same-Origin Policy, which prevents JavaScript
 * code running on one origin (domain/port/protocol) from making requests to a different
 * origin. Since our frontend and backend run on different ports (3000 vs 8080), they
 * are considered different origins, and browsers would block these requests without
 * proper CORS configuration.
 * 
 * Without this configuration, you would see errors like:
 * - "Access to fetch at 'http://localhost:8080/api/stock/...' from origin 
 *   'http://localhost:3000' has been blocked by CORS policy"
 * - "No 'Access-Control-Allow-Origin' header is present on the requested resource"
 * 
 * This configuration tells the browser that it's safe to allow the frontend to make
 * requests to this backend API.
 * 
 * SECURITY NOTE:
 * ==============
 * This configuration is set up for development. In production, you should:
 * 1. Replace wildcard origins with specific production frontend URLs
 * 2. Consider restricting allowed methods to only what's needed (GET, POST, etc.)
 * 3. Review and limit allowed headers to only necessary ones
 * 4. Ensure proper authentication and authorization are in place
 */
@Configuration
public class CorsConfig {

    /**
     * Creates and configures a CORS filter bean that will be applied to all HTTP requests.
     * This filter intercepts requests and adds the necessary CORS headers to responses.
     * 
     * @return CorsFilter configured to allow cross-origin requests from the frontend
     */
    @Bean
    public CorsFilter corsFilter() {
        // Create a source that maps URL patterns to CORS configurations
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Create a new CORS configuration object
        CorsConfiguration config = new CorsConfiguration();
        
        /**
         * ALLOWED ORIGINS:
         * Specify which origins (frontend URLs) are allowed to make requests to this API.
         * Both localhost and 127.0.0.1 are included to handle different browser behaviors.
         * 
         * In production, replace these with your actual frontend domain(s):
         * Example: config.addAllowedOrigin("https://yourdomain.com");
         */
        config.addAllowedOrigin("http://localhost:3000");  // Vite dev server default port
        config.addAllowedOrigin("http://127.0.0.1:3000");  // Alternative localhost format
        
        /**
         * ALLOWED HTTP METHODS:
         * Specify which HTTP methods (GET, POST, PUT, DELETE, etc.) are allowed.
         * Using "*" allows all methods, which is convenient for development.
         * 
         * For production, consider restricting to only needed methods:
         * Example: config.addAllowedMethod("GET");
         *          config.addAllowedMethod("POST");
         */
        config.addAllowedMethod("*");
        
        /**
         * ALLOWED HEADERS:
         * Specify which HTTP headers can be sent with the request.
         * Using "*" allows all headers, which is convenient for development.
         * 
         * Common headers include: Content-Type, Authorization, X-Requested-With, etc.
         * For production, consider specifying only necessary headers.
         */
        config.addAllowedHeader("*");
        
        /**
         * ALLOW CREDENTIALS:
         * When set to true, allows cookies, authorization headers, and TLS client
         * certificates to be included in cross-origin requests.
         * 
         * IMPORTANT: When allowCredentials is true, you cannot use "*" for allowedOrigins.
         * You must specify exact origins (which we do above).
         */
        config.setAllowCredentials(true);
        
        /**
         * REGISTER CORS CONFIGURATION:
         * Apply this CORS configuration to all URL patterns ("/**" means all paths).
         * This means all endpoints in the API will accept cross-origin requests
         * according to the rules defined above.
         */
        source.registerCorsConfiguration("/**", config);
        
        // Return the configured CORS filter
        return new CorsFilter(source);
    }
}

