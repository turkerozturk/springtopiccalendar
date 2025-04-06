package turkerozturk.ptt.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {

    String message() default "Geçersiz tarih aralığı!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
