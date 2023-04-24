package com.seproject.oauth2.utils.handler;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class FormLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        if(exception instanceof BadCredentialsException) {
            response.addHeader("message",exception.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        } else {
            response.sendError(HttpStatus.BAD_REQUEST.value());
        }


    }
}
