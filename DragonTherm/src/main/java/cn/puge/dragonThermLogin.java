package cn.puge;

import cn.puge.request.LoginRequest;
import cn.puge.response.LoginResponse;
//import cn.puge.easycase.service.IPugeCaseSVC;
import cn.puge.service.HttpServer;
import cn.puge.service.LoginService;

/**
 * 火龙果智能温控系统 - 登录模块
 * 主程序入口类
 *
 * 启动模式：
 *   - 不带参数(默认)      ：启动HTTP服务器，浏览器访问 http://localhost:8080
 *   - 第一个参数为端口号      ：启动HTTP服务器，指定端口
 *   - 第一个参数为 console : 启动控制台模式演示
 *
 * 功能说明：
 * 1. 手机号验证码登录
 * 2. 微信登录
 * 3. 账号密码登录
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class dragonThermLogin {

    /**
     * 登录服务实例
     */
    private static final LoginService LOGIN_SERVICE = new LoginService();

    /**
     * 主程序入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 优先判断是否启动控制台测试模式
        if (args.length > 0 && "console".equalsIgnoreCase(args[0])) {
            runConsoleDemo();
            return;
        }

        // 否则启动 HTTP 服务器（可带端口号参数）
        HttpServer.main(args);
    }

    /**
     * 控制台演示模式：演示三种登录方式
     */
    private static void runConsoleDemo() {
        System.out.println("===========================================");
        System.out.println("  火龙果智能温控系统 - 登录模块 v1.0");
        System.out.println("===========================================\n");

        testPasswordLogin();
        testSmsCodeLogin();
        testWechatLogin();

        System.out.println("\n===========================================");
        System.out.println("  测试完成");
        System.out.println("===========================================");
    }

    /**
     * 测试账号密码登录
     */
    private static void testPasswordLogin() {
        System.out.println("【测试】账号密码登录");
        System.out.println("- - - - - - - - - - - - - - - - - - - - -");

        // 创建登录请求（账号1）
        LoginRequest request1 = new LoginRequest();
        request1.setLoginType(LoginRequest.LoginType.PASSWORD.getCode());
        request1.setPhone("13546069966");
        request1.setPassword("123456");
        request1.setClientIp("127.0.0.1");

        // 执行登录
        LoginResponse response1 = LOGIN_SERVICE.login(request1);
        System.out.println("用户一登录结果: " + response1);
        System.out.println();

        // 创建登录请求（账号2 - 错误密码）
        LoginRequest request2 = new LoginRequest();
        request2.setLoginType(LoginRequest.LoginType.PASSWORD.getCode());
        request2.setPhone("13935193040");
        request2.setPassword("wrong_password");
        request2.setClientIp("127.0.0.1");

        // 执行登录
        LoginResponse response2 = LOGIN_SERVICE.login(request2);
        System.out.println("用户二（错误密码）登录结果: " + response2);
        System.out.println();

        // 创建登录请求（账号3 - 不存在的用户）
        LoginRequest request3 = new LoginRequest();
        request3.setLoginType(LoginRequest.LoginType.PASSWORD.getCode());
        request3.setPhone("18888888888");
        request3.setPassword("123456");
        request3.setClientIp("127.0.0.1");

        // 执行登录
        LoginResponse response3 = LOGIN_SERVICE.login(request3);
        System.out.println("用户三（不存在）登录结果: " + response3);
        System.out.println();
    }

    /**
     * 测试手机号验证码登录
     */
    private static void testSmsCodeLogin() {
        System.out.println("【测试】手机号验证码登录");
        System.out.println("- - - - - - - - - - - - - - - - - - - - -");

        // 1. 先发送验证码
        String phone = "13546069966";
        System.out.println("步骤1: 发送验证码到手机号 " + phone);
        try {
            String smsCode = LOGIN_SERVICE.sendSmsCode(phone);
            System.out.println("验证码已发送（模拟）: " + smsCode);
        } catch (Exception e) {
            System.out.println("发送失败: " + e.getMessage());
            return;
        }
        System.out.println();

        // 2. 使用正确验证码登录
        System.out.println("步骤2: 使用正确验证码登录");
        LoginRequest request1 = new LoginRequest();
        request1.setLoginType(LoginRequest.LoginType.SMS_CODE.getCode());
        request1.setPhone(phone);
        request1.setSmsCode("获取的验证码"); // 实际使用时填入真实验证码
        request1.setClientIp("127.0.0.1");

        // 执行登录（预期失败，因为验证码需要真实获取）
        // 这里演示正确的使用流程
        System.out.println("提示: 实际使用时验证码由sendSmsCode方法返回");
        System.out.println();

        // 演示：模拟正确验证码登录流程
        System.out.println("步骤3: 模拟正确验证码登录流程");
        try {
            String realCode = LOGIN_SERVICE.sendSmsCode("13935193040");
            LoginRequest request2 = new LoginRequest();
            request2.setLoginType(LoginRequest.LoginType.SMS_CODE.getCode());
            request2.setPhone("13935193040");
            request2.setSmsCode(realCode);
            request2.setClientIp("127.0.0.1");

            LoginResponse response = LOGIN_SERVICE.login(request2);
            System.out.println("模拟登录结果: " + response);
        } catch (Exception e) {
            System.out.println("登录失败: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 测试微信登录
     */
    private static void testWechatLogin() {
        System.out.println("【测试】微信登录");
        System.out.println("- - - - - - - - - - - - - - - - - - - - -");

        // 方式1：通过授权码登录（模拟）
        System.out.println("方式1: 通过授权码登录");
        LoginRequest request1 = new LoginRequest();
        request1.setLoginType(LoginRequest.LoginType.WECHAT.getCode());
        request1.setWechatCode("WECHAT_TEST_CODE_12345");
        request1.setClientIp("127.0.0.1");

        LoginResponse response1 = LOGIN_SERVICE.login(request1);
        System.out.println("授权码登录结果: " + response1);
        System.out.println();

        // 方式2：通过OpenId登录（已授权用户）
        System.out.println("方式2: 通过OpenId登录");
        LoginRequest request2 = new LoginRequest();
        request2.setLoginType(LoginRequest.LoginType.WECHAT.getCode());
        request2.setWechatOpenId("oABC123DEF456");
        request2.setClientIp("127.0.0.1");

        LoginResponse response2 = LOGIN_SERVICE.login(request2);
        System.out.println("OpenId登录结果: " + response2);
        System.out.println();
    }

    // ==================== 对外提供的静态方法 ====================

    /**
     * 用户登录接口（供外部调用）
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    public static LoginResponse dragonThermLogin(LoginRequest request) {
        return LOGIN_SERVICE.login(request);
    }

    /**
     * 发送短信验证码接口（供外部调用）
     *
     * @param phone 手机号
     * @return 发送的验证码
     */
    public static String sendSmsCode(String phone) {
        return LOGIN_SERVICE.sendSmsCode(phone);
    }
}
