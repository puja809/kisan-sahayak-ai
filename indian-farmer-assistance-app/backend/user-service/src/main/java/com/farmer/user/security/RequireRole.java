package com.farmer.user.security;

import com.farmer.user.entity.User;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for method-level role-based access control.
 * Use this annotation to require specific roles for accessing methods.
 *
 * Example usage:
 * <pre>
 * {@code
 * @RequireRole(User.Role.ADMIN)
 * public void adminOnlyMethod() {
 *     // Only admins can access this method
 * }
 *
 * @RequireRole(value = {User.Role.ADMIN, User.Role.FARMER})
 * public void sharedMethod() {
 *     // Both admins and farmers can access this method
 * }
 * }
 * </pre>
 *
 * Requirements: 22.3, 22.4, 22.5
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * The roles that are allowed to access the annotated method.
     * If multiple roles are specified, any of them can access the method.
     */
    User.Role[] value() default {};

    /**
     * Whether all specified roles are required (AND logic).
     * If false, any of the specified roles can access (OR logic).
     */
    boolean requireAll() default false;
}