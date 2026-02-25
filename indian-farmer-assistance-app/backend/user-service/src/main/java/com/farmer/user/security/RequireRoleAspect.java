package com.farmer.user.security;

import com.farmer.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aspect for enforcing @RequireRole annotation on methods.
 * Provides method-level role-based access control.
 * Requirements: 22.3, 22.4, 22.5
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequireRoleAspect {

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access attempt to: {}", joinPoint.getSignature());
            throw new AccessDeniedException("Authentication required");
        }

        User.Role[] requiredRoles = requireRole.value();
        if (requiredRoles.length == 0) {
            // No role requirement specified, allow access
            return joinPoint.proceed();
        }

        String userId = authentication.getName();
        Set<String> userRoles = getUserRoles(authentication);

        boolean hasAccess;
        if (requireRole.requireAll()) {
            // AND logic - user must have ALL specified roles
            Set<String> requiredRoleSet = Arrays.stream(requiredRoles)
                    .map(User.Role::name)
                    .collect(Collectors.toSet());
            hasAccess = userRoles.containsAll(requiredRoleSet);
        } else {
            // OR logic - user must have ANY of the specified roles
            hasAccess = Arrays.stream(requiredRoles)
                    .anyMatch(role -> userRoles.contains(role.name()));
        }

        if (!hasAccess) {
            log.warn("Access denied for user {} to method {}. Required roles: {}, User roles: {}",
                    userId,
                    joinPoint.getSignature(),
                    Arrays.toString(requiredRoles),
                    userRoles);

            throw new AccessDeniedException(
                    "Access denied. Required role(s): " + Arrays.toString(requiredRoles));
        }

        log.debug("Access granted for user {} to method {}", userId, joinPoint.getSignature());
        return joinPoint.proceed();
    }

    /**
     * Get all roles from authentication.
     */
    private Set<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toSet());
    }
}