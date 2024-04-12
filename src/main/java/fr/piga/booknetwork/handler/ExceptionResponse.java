package fr.piga.booknetwork.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY) // Don't include null values in JSON response body if they are empty
public class ExceptionResponse {

    private Integer businessErrorCode;

    private String businessErrorDescription;

    private String error;

    private Set<String> validationErrors;

    private Map<String, String> errors;
}
