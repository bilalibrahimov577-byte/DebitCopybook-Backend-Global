package com.example.DebitCopybook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Sistemdə axtarılan istifadəçi tapılmadıqda bu xətanı atmaq üçün istifadə olunur.
 * @ResponseStatus(HttpStatus.NOT_FOUND) annotasiyası sayəsində, bu xəta baş verdikdə
 * avtomatik olaraq client-ə HTTP 404 (Not Found) status kodu qaytarılacaq.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}