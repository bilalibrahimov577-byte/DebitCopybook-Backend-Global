package com.example.DebitCopybook.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;


@Component
public class ApiKeyInterceptor implements HandlerInterceptor {


    @Value("${api.secret-key}")
    private String secretApiKey;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        String requestApiKey = request.getHeader("X-API-KEY");


        if (secretApiKey.equals(requestApiKey)) {
            return true;
        } else {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized Access: Invalid or missing API Key.");
            return false;
        }
    }
}