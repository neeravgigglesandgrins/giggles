package com.giggles.auth.filter;

import com.giggles.auth.entity.UserEntity;
import com.giggles.auth.entity.UserSessionEntity;
import com.giggles.auth.enums.UserSessionStatus;
import com.giggles.auth.repository.UserSessionRepository;
import com.giggles.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String token = authHeader.substring(7);
            
            // Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid JWT token");
                filterChain.doFilter(request, response);
                return;
            }
            
            // Check if token exists in database and is valid
            UserSessionEntity session = userSessionRepository.findByToken(token)
                    .orElse(null);
            
            if (session == null || 
                !session.getUserSessionStatus().equals(UserSessionStatus.VALID) ||
                session.getExpiry().isBefore(LocalDateTime.now()) ||
                session.getDeleted()) {
                log.warn("Token session not found or expired");
                filterChain.doFilter(request, response);
                return;
            }
            
            // Get user
            UserEntity user = session.getUser();
            if (user == null || user.getDeleted()) {
                log.warn("User not found or deleted");
                filterChain.doFilter(request, response);
                return;
            }
            
            // Set authentication
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Store user in request attribute for easy access
            request.setAttribute("userId", user.getId());
            request.setAttribute("user", user);
            
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}

