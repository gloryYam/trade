package trade.tradestream.common.dto;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiResponse<T> {

    private int code;
    private HttpStatus status;
    private String message;
    private T data;

    public ApiResponse(HttpStatus status, String message, T data) {
        this.code = status.value();
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(HttpStatus status, String message) {
        this.code = status.value();
        this.status = status;
        this.message = message;
        this.data = null;
    }

    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    public static <T> ApiResponse<T> of(HttpStatus httpStatus, T data) {
        return of(httpStatus, httpStatus.name(), data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(HttpStatus.OK, HttpStatus.OK.name(), data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(status, message);
    }
}
