package fr.piga.booknetwork.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum BusinessErrorCodes {  // Ici on a une énumération des codes d'erreurs métier de l'application

    NO_CODE(0, NOT_IMPLEMENTED,"No code"),

    INCORRECT_CURRENT_PASSWORD(300, BAD_REQUEST,"Incorrect current password"),

    NEW_PASSWORD_DOES_NOT_MATCH(301, BAD_REQUEST,"New password does not match"),

    ACCOUNT_LOCKED(302, FORBIDDEN,"Account is locked"),

    ACCOUNT_DISABLED(303, FORBIDDEN,"Account is disabled"),

    BAD_CREDENTIALS(304, FORBIDDEN,"Login or / and Password is incorrect");


    private final int code;

    private final  String description;

    private final HttpStatus httpStatus;

    BusinessErrorCodes(int code,HttpStatus httpStatus ,String description ) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }
}
