package eu.efti.commons.dto;

import eu.efti.commons.enums.ErrorCodesEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto {
    private int id;
    private String errorCode;
    private String errorDescription;

    public static ErrorDto fromErrorCode(final ErrorCodesEnum errorCode) {
        return ErrorDto.builder()
                .errorCode(errorCode.name())
                .errorDescription(errorCode.getMessage()).build();
    }

    public static ErrorDto fromAnyError(final String errorDescription) {
        return ErrorDto.builder()
                .errorCode(ErrorCodesEnum.DEFAULT_ERROR.name())
                .errorDescription(errorDescription).build();
    }
}
