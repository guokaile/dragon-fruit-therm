package cn.puge.service;

import cn.puge.request.LoginRequest;
import cn.puge.response.LoginResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 简易HTTP服务器
 * 提供静态文件服务和登录接口
 *
 * 接口说明：
 *   GET  /              -> 返回登录页面
 *   GET  /login.html    -> 返回登录页面
 *   POST /api/sendSms   -> 发送短信验证码 (参数: phone)
 *   POST /api/login     -> 登录接口
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class HttpServer {

    private static final int PORT = 8080;
    private static final String STATIC_ROOT = "web";
    private static final LoginService loginService = new LoginService();

    /**
     * 获取静态文件根目录
     * 优先使用命令行指定路径，其次使用 user.dir，再次使用 classpath
     */
    private static String resolveStaticRoot() {
// 1. 检查当前工作目录下是否存在 web 文件夹
        File f1 = new File(STATIC_ROOT);
        if (f1.exists() && f1.isDirectory()) {
            return f1.getAbsolutePath();
        }

        // 2. 检查 user.dir 指定的目录下是否存在
        String userDir = System.getProperty("user.dir", "");
        if (!userDir.isEmpty()) {
            File f2 = new File(userDir, STATIC_ROOT);
            if (f2.exists() && f2.isDirectory()) {
                return f2.getAbsolutePath();
            }
        }

        // 3. 从 class 文件位置向上查找 web 目录
        //    同时检查每层目录的兄弟目录（解决 IDE 将 class 输出到 out/ 而 web 在 DragonTherm/ 的问题）
        try {
            java.net.URL classUrl = HttpServer.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            if (classUrl != null) {
                File current = new File(classUrl.toURI());
                for (int i = 0; i < 8 && current != null; i++) {
                    // 3a. 检查当前目录自身
                    File webDir = new File(current, STATIC_ROOT);
                    if (webDir.exists() && webDir.isDirectory()) {
                        return webDir.getAbsolutePath();
                    }
                    // 3b. 检查当前目录的直接子目录（覆盖兄弟目录场景）
                    File[] children = current.listFiles(File::isDirectory);
                    if (children != null) {
                        for (File child : children) {
                            File siblingWeb = new File(child, STATIC_ROOT);
                            if (siblingWeb.exists() && siblingWeb.isDirectory()) {
                                return siblingWeb.getAbsolutePath();
                            }
                        }
                    }
                    current = current.getParentFile();
                }
            }
        } catch (Exception ignored) {
            // 忽略 classpath 查找失败
        }

        // 4. 返回当前工作目录下的路径（兜底）
        return new File(STATIC_ROOT).getAbsolutePath();
    }

    private static String getStaticRoot() {
        return resolveStaticRoot();
    }

    /**
     * 启动HTTP服务器
     *
     * @param args 命令行参数（可传入端口号）
     */
    public static void main(String[] args) {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("参数无效，使用默认端口: " + PORT);
            }
        }

        com.sun.net.httpserver.HttpServer server;
        try {
            server = com.sun.net.httpserver.HttpServer.create(
                    new InetSocketAddress(port), 0);
        } catch (IOException e) {
            System.out.println("启动服务器失败: " + e.getMessage());
            return;
        }

// 注册处理器
        server.createContext("/", HttpServer::handleRequest);
        server.setExecutor(null); // 使用默认executor
        server.start();

        String staticRoot = getStaticRoot();
        System.out.println("===========================================");
        System.out.println("  火龙果智能温控系统 - 登录服务已启动");
        System.out.println("  访问地址: http://localhost:" + port);
        System.out.println("  静态资源路径: " + staticRoot);
        System.out.println("===========================================");
    }

    /**
     * 处理请求
     */
    private static void handleRequest(com.sun.net.httpserver.HttpExchange exchange)
            throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

// 设置通用响应头（解决跨域问题，同源时也不影响）
        exchange.getResponseHeaders().add("Content-Type",
                "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");

        try {
// 路由分发
            switch (path) {
                case "/api/sendSms":
                    if ("POST".equals(method)) {
                        handleSendSms(exchange);
                    } else {
                        writeResponse(exchange, 405, "{\"code\":405,\"message\":\"方法不允许\"}");
                    }
                    break;

                case "/api/login":
                    if ("POST".equals(method)) {
                        handleLogin(exchange);
                    } else {
                        writeResponse(exchange, 405, "{\"code\":405,\"message\":\"方法不允许\"}");
                    }
                    break;

                case "/":
                case "/login.html":
                    if ("GET".equals(method)) {
                        serveStaticFile(exchange, "/login.html");
                    } else {
                        writeResponse(exchange, 405, "{\"code\":405,\"message\":\"方法不允许\"}");
                    }
                    break;

                default:
// 其他请求作为静态文件处理
                    if ("GET".equals(method)) {
                        serveStaticFile(exchange, path);
                    } else {
                        writeResponse(exchange, 404, "{\"code\":404,\"message\":\"资源不存在\"}");
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("处理请求异常: " + e.getMessage());
            e.printStackTrace();
            writeResponse(exchange, 500, "{\"code\":500,\"message\":\"系统异常\"}");
        }
    }

    /**
     * 处理发送短信验证码请求
     */
    private static void handleSendSms(com.sun.net.httpserver.HttpExchange exchange)
            throws IOException {
// 读取请求体
        String body = readRequestBody(exchange);

// 解析参数（支持 form-urlencoded 和 json 格式）
        String phone = null;
        if (body.startsWith("{")) {
            phone = extractJsonField(body, "phone");
        } else {
            Map<String, String> params = parseFormBody(body);
            phone = params.get("phone");
        }

// 简单的参数校验
        if (phone == null || phone.trim().isEmpty()) {
            writeResponse(exchange, 200, "{\"code\":400,\"message\":\"手机号不能为空\",\"success\":false}");
            return;
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            writeResponse(exchange, 200, "{\"code\":400,\"message\":\"手机号格式不正确\",\"success\":false}");
            return;
        }

        try {
// 调用业务服务发送验证码
            String code = loginService.sendSmsCode(phone);
            writeResponse(exchange, 200,
                    "{\"code\":200,\"message\":\"发送成功\",\"success\":true,\"data\":\""
                            + code + "\"}");
        } catch (Exception e) {
            writeResponse(exchange, 200,
                    "{\"code\":400,\"message\":\"" + escapeJson(e.getMessage())
                            + "\",\"success\":false}");
        }
    }

    /**
     * 处理登录请求
     */
    private static void handleLogin(com.sun.net.httpserver.HttpExchange exchange)
            throws IOException {
        String body = readRequestBody(exchange);

// 解析参数
        Integer loginType = null;
        String phone = null;
        String password = null;
        String smsCode = null;
        String wechatCode = null;
        String wechatOpenId = null;

        if (body.startsWith("{")) {
            loginType = parseInteger(extractJsonField(body, "loginType"));
            phone = extractJsonField(body, "phone");
            password = extractJsonField(body, "password");
            smsCode = extractJsonField(body, "smsCode");
            wechatCode = extractJsonField(body, "wechatCode");
            wechatOpenId = extractJsonField(body, "wechatOpenId");
        } else {
            Map<String, String> params = parseFormBody(body);
            loginType = parseInteger(params.get("loginType"));
            phone = params.get("phone");
            password = params.get("password");
            smsCode = params.get("smsCode");
            wechatCode = params.get("wechatCode");
            wechatOpenId = params.get("wechatOpenId");
        }

// 构造登录请求
        LoginRequest request = new LoginRequest();
        if (loginType != null) {
            request.setLoginType(loginType);
        }
        request.setPhone(phone);
        request.setPassword(password);
        request.setSmsCode(smsCode);
        request.setWechatCode(wechatCode);
        request.setWechatOpenId(wechatOpenId);
        request.setClientIp(exchange.getRemoteAddress().getAddress().getHostAddress());

// 调用业务服务
        LoginResponse response = loginService.login(request);

// 转换为JSON字符串
        writeResponse(exchange, 200, toJson(response));
    }

    /**
     * 服务静态文件
     */
    private static void serveStaticFile(com.sun.net.httpserver.HttpExchange exchange,
                                        String path) throws IOException {
// 规范化路径（避免路径遍历攻击）
        if (path.contains("..")) {
            writeResponse(exchange, 403, "{\"code\":403,\"message\":\"禁止访问\"}");
            return;
        }

        String filename = path;
        if ("/".equals(filename)) {
            filename = "/login.html";
        }

// 确定文件类型和Content-Type
        String contentType;
        if (filename.endsWith(".html")) {
            contentType = "text/html; charset=utf-8";
        } else if (filename.endsWith(".css")) {
            contentType = "text/css; charset=utf-8";
        } else if (filename.endsWith(".js")) {
            contentType = "application/javascript; charset=utf-8";
        } else if (filename.endsWith(".png") || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg") || filename.endsWith(".gif")
                || filename.endsWith(".ico")) {
            contentType = "image/*";
        } else {
            contentType = "application/octet-stream";
        }

// 尝试读取文件
        byte[] content = readStaticFile(filename);
        if (content == null) {
            writeResponse(exchange, 404,
                    "<html><head><meta charset='utf-8'><title>404</title></head>"
                            + "<body style='padding:40px;text-align:center;font-family:sans-serif;'>"
                            + "<h1>404 - 资源不存在</h1><p>找不到: " + filename + "</p></body></html>");
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    /**
     * 读取静态文件内容
     * 优先从磁盘文件系统读取，找不到则尝试从classpath中读取
     */
    private static byte[] readStaticFile(String path) {
// 去除前缀斜杠
        String relative = path.startsWith("/") ? path.substring(1) : path;

// 先尝试从 web 文件夹读取（基于 resolved root）
        File file = new File(getStaticRoot(), relative);
        if (file.exists() && file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                System.out.println("读取文件失败: " + file.getAbsolutePath());
                return null;
            }
        }

// 其次尝试从classpath读取（jar或 IDE out 目录下 resources）
        try (InputStream is = HttpServer.class.getClassLoader().getResourceAsStream(
                "web/" + relative)) {
            if (is != null) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, n);
                    }
                    return baos.toByteArray();
                }
            }
        } catch (IOException e) {
// 忽略
        }

        return null;
    }

    /**
     * 读取请求体
     */
    private static String readRequestBody(com.sun.net.httpserver.HttpExchange exchange)
            throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * 解析 form-urlencoded 格式请求体
     */
    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.trim().isEmpty()) {
            return params;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * 简单提取JSON字段值（非严格解析，仅处理普通字符串和数字）
     */
    private static String extractJsonField(String json, String field) {
        if (json == null) {
            return null;
        }
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
// 尝试数字
        pattern = "\"" + field + "\"\\s*:\\s*(-?\\d+)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将LoginResponse转换为JSON字符串（手写JSON避免依赖）
     */
    private static String toJson(LoginResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"code\":").append(response.getCode()).append(",");
        sb.append("\"success\":").append(response.getSuccess()).append(",");
        sb.append("\"message\":\"").append(escapeJson(response.getMessage())).append("\"");

        if (response.getSuccess() && response.getUserInfo() != null) {
            sb.append(",\"accessToken\":\"").append(escapeJson(response.getAccessToken())).append("\"");
            sb.append(",\"expiresIn\":").append(response.getExpiresIn());

            LoginResponse.UserInfo userInfo = response.getUserInfo();
            sb.append(",\"userInfo\":{");
            sb.append("\"userId\":\"").append(escapeJson(userInfo.getUserId())).append("\",");
            sb.append("\"phone\":\"").append(escapeJson(userInfo.getPhone())).append("\",");
            sb.append("\"nickname\":\"").append(escapeJson(userInfo.getNickname())).append("\",");
            sb.append("\"avatarUrl\":\"").append(escapeJson(userInfo.getAvatarUrl())).append("\"");
            sb.append("}");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * JSON字符串转义
     */
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 写响应
     */
    private static void writeResponse(com.sun.net.httpserver.HttpExchange exchange,
                                      int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}