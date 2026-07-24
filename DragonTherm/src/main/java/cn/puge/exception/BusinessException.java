package cn.puge.exception;

/**
 * 业务异常基类
 * 用于处理登录过程中的各种业务逻辑异常
 *
 * @author Puge
 * @since 1.0
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 默认构造器
     */
    public BusinessException() {
        super();
    }

    /**
     * 带错误信息的构造器
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 带错误码和错误信息的构造器
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 带错误信息和cause的构造器
     *
     * @param message 错误信息
     * @param cause   异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带错误码、错误信息和cause的构造器
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     异常原因
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
