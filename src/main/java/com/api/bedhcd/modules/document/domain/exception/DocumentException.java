package com.api.bedhcd.modules.document.domain.exception;
import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;
public class DocumentException extends BaseDomainException {
    public DocumentException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }
    public static DocumentException notFound(String id) {
        return new DocumentException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu: " + id);
    }
}
