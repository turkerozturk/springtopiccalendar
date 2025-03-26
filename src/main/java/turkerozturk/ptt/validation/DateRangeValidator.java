package turkerozturk.ptt.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import turkerozturk.ptt.dto.FilterDto;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, FilterDto> {

    @Override
    public boolean isValid(FilterDto filterDto, ConstraintValidatorContext context) {
        if (filterDto.getStartDate() == null || filterDto.getEndDate() == null) {
            // null ise kontrol etme, isterseniz false döndürüp "Zorunlu alan" diyebilirsiniz
            return true;
        }
        return !filterDto.getStartDate().isAfter(filterDto.getEndDate());
    }
}
