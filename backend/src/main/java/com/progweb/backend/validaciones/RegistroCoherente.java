package com.progweb.backend.validaciones;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorRegistro.class)
public @interface RegistroCoherente {
    String message() default "Revisa los datos del registro.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
