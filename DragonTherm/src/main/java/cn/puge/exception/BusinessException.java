package cn.puge.exception;

/**
 * 业务异常基类
 * 用于处理登录过程中的各种业务逻辑异常
 *
 * @author guokaile
 * @since 1.0
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private String errorCode;

    //BusinessException的构造方法的重载

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
    //运用cause参数，是为了方便日志排查时快速定位异常位置，cause参数可以包含异常的详细信息为了后端开发者定位问题
    //errorCode是用来标识异常的类型，方便前端根据异常类型进行处理和提示，方便前端开发及运维定位问题
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


    //错误码的getter和setter方法
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    // toString （大厂规范），是为了日志排查时快速定位业务错误类型
    @Override
    public String toString() {
        return "BusinessException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';


    }
}