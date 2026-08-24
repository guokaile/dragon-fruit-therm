package cn.puge.service;

import cn.puge.exception.BusinessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务
 * 负责验证码的生成、存储和校验
 *
 * 发送模式：
 *   1. 真实短信 — 读取 sms-config.properties，通过 HTTPS 调用阿里云短信API
 *   2. 模拟模式 — 未配置或配置错误时，控制台打印验证码（开发/演示用）
 *
 * 技术实现：
 *   使用 JDK 内置 HttpURLConnection + HMAC-SHA1 签名，零外部依赖。
 *   签名算法遵循阿里云 POP API 规范（RFC 3986 URL编码 + HMAC-SHA1）。
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class SmsCodeService {

    /** 验证码有效时间（毫秒） */
    //有效时间为1分钟
    private static final long SMS_CODE_EXPIRE_TIME = 60 * 1000;

    /** 验证码存储 */
    private static final Map<String, SmsCodeInfo> SMS_CODE_MAP = new ConcurrentHashMap<>();

    // ==================== 阿里云短信配置 ====================

    //smsEnabled 是短信发送的「全局开关变量」，用来控制「走真实阿里云短信API」还是「走模拟模式（开发/测试用）」
    private static boolean smsEnabled = false;
    private static String accessKeyId;
    private static String accessKeySecret;
    private static String signName;
    private static String templateCode;
    private static String simulationReason = "未配置 sms-config.properties";

    /** 阿里云短信 API 域名 */
    private static final String SMS_ENDPOINT = "https://dysmsapi.aliyuncs.com/";

    //用 static 关键字修饰的代码块，没有方法名、没有参数、没有返回值，用来初始化静态变量，类加载时自动执行，且只执行1次
    static {
        loadSmsConfig();
    }

    // ==================== 配置加载 ====================

    /*
    * 从多个可能路径加载 sms-config.properties
    * 如果未找到或配置错误，使用模拟模式（开发/演示用）
    * 如果配置正确，加载到静态变量中
    * @param props
    * @return void
    * */

    /** loadSmsConfig 方法用来加载短信服务的配置 */
    private static void loadSmsConfig() {
        //Properties props 是Java内置的「配置文件专用键值对容器」，专门用来存储 .properties 配置文件里的配置项（比如你项目里的短信服务配置），
        //是Java标准库的一部分，不需要额外引入依赖
        //loadPropertiesFromDisk 方法会从多个可能路径加载 sms-config.properties 文件
        //如果未找到或配置错误，返回一个空的 Properties 对象，如果配置正确，返回一个非空的 Properties 对象
        Properties props = loadPropertiesFromDisk();
        //trim() 是 String 类的方法，isEmpty() 是 Collection/Map 类的方法
        //isEmpty()判断「容器/对象是否没有任何内容（字符/元素/键值对）」
        if (props.isEmpty()) {
            System.out.println("[短信服务] 未找到 sms-config.properties，使用模拟模式");
            //如果未找到或配置错误，直接返回，不加载到静态变量中
            //return是大厂强制要求的「提前返回（Early Return）」代码规范——作用是「提前终止方法执行，避免空配置的后续错误处理」
            return;
        }

        //Properties 作为「配置文件专用容器」，内置了两套get方法：
        // 一套是继承自Hashtable的通用get方法（所有Map都有），另一套是Properties专属的getProperty方法（专为配置文件设计）
        accessKeyId = props.getProperty("accessKeyId");
        accessKeySecret = props.getProperty("accessKeySecret");
        signName = props.getProperty("signName");
        templateCode = props.getProperty("templateCode");

        //isEmpty() 是 Collection 类的方法，判断「容器/对象是否没有任何内容（字符/元素/键值对）」
        //如果 accessKeyId 或 accessKeySecret 为空，说明配置错误，直接返回，不加载到静态变量中
        //如果值为空，isEmpty(accessKeyId) 返回 true
        if (isEmpty(accessKeyId) || isEmpty(accessKeySecret)) {
            simulationReason = "sms-config.properties 中缺少 accessKeyId 或 accessKeySecret";
            System.out.println("[短信服务] " + simulationReason);
            return;
        }
        if (isEmpty(signName) || "你的签名名称".equals(signName)) {
            simulationReason = "请在 sms-config.properties 中填写真实的 signName（审核通过的签名）";
            System.out.println("[短信服务] " + simulationReason);
            return;
        }
        if (isEmpty(templateCode) || "你的模板CODE".equals(templateCode)) {
            simulationReason = "请在 sms-config.properties 中填写真实的 templateCode（审核通过的模板CODE）";
            System.out.println("[短信服务] " + simulationReason);
            return;
        }

        smsEnabled = true;
        System.out.println("[短信服务] ✅ 阿里云短信配置加载成功");
        //取AccessKeyId的前8位（Java substring(start, end) 左闭右开，取索引0-7的8个字符），用3个星号做「脱敏掩码」
        //这是为了保护敏感信息，避免在日志中直接暴露，进行脱敏处理，只显示前8位，后4位用星号代替
        System.out.println("[短信服务]    AccessKeyId: " + accessKeyId.substring(0, 8) + "***");
        System.out.println("[短信服务]    签名: " + signName);
        System.out.println("[短信服务]    模板: " + templateCode);
    }

    /**
     * 从多个可能路径加载 sms-config.properties
     */
    private static Properties loadPropertiesFromDisk() {
        Properties props = new Properties();
        //Java 数组的「静态初始化」语法——是 Java 专门为「固定大小、已知所有元素」的数组设计的简洁写法，类型[] 数组名 = { 元素1, 元素2, 元素3, ... };
        /* 核心特点：
         ① 不需要用new关键字（和普通对象/数组的new初始化不同）
         ② 直接用大括号{}把所有元素列出来
         ③ 数组大小由元素个数自动决定（这里3个元素，数组大小就是3）
        * */
        String[] searchPaths = {
                "sms-config.properties",                              // 当前目录
                System.getProperty("user.dir") + "/sms-config.properties", // user.dir
                "DragonTherm/sms-config.properties",                  // 项目子目录
        };

        //遍历 searchPaths 数组，尝试从每个路径加载 sms-config.properties 文件
        //如果成功加载，返回非空的 Properties 对象
        //如果所有路径都失败，返回空的 Properties 对象
        for (String path : searchPaths) {
            try {
                //File 是 Java 文件系统的抽象句柄——专门用来操作「文件/目录的路径、属性、状态」，不负责读写文件内容（读写内容要用「流类」），是Java IO体系的基础类之一
                File f = new File(path);
                //判断配置文件存在且是文件类型（不是目录）
                if (f.exists() && f.isFile()) {
                    //FileInputStream是Java IO体系中字节输入流的核心类，作用是「在磁盘文件和内存之间建立一条字节传输通道」，专门用来读取文件的原始字节数据
                    //创建「指向配置文件的字节输入流」——用来把磁盘上的配置文件内容以字节形式读取到内存，后续交给Properties类解析成「键值对配置项」
                    try (FileInputStream fis = new FileInputStream(f)) {
                        //把字节流的文件内容解析成Properties的键值对
                        props.load(fis);
                        //f.getAbsolutePath() 是 File类用来「获取文件的绝对路径字符串**」——专门解决「相对路径变绝对路径」的问题
                        //项目里的作用是把配置文件的加载路径打印到日志里，方便开发者快速定位配置文件的实际位置，是日志调试的核心工具方法
                        System.out.println("[短信服务] 从磁盘加载配置: " + f.getAbsolutePath());
                        //return props 是方法「契约的强制要求」——因为 loadPropertiesFromDisk 方法的设计目的就是「加载配置文件并返回Properties对象」
                        //如果不返回，调用方（loadSmsConfig）就拿不到加载的配置，整个配置加载逻辑会失效
                        return props;
                    }
                }
                //Java异常处理「捕获异常但不做任何处理」的特殊写法，用于忽略异常，继续执行后续代码
                //如果不吞噬异常，单个路径失败会导致整个方法崩溃，如果路径1失败，路径2、3根本不会被尝试，容错逻辑失效
            } catch (Exception ignored) {}
        }

        // 尝试 classpath
        //从classpath（编译后的资源目录）加载默认配置文件
        //这是之前「磁盘路径加载配置」的兜底容错方案，专门解决「项目打包成jar后，磁盘路径的配置文件可能不存在」的问题
        //getClassLoader() 是 Java 类加载机制的核心方法，作用是拿到负责加载classpath下所有资源的「加载器」
        //getResourceAsStream() 是 ClassLoader 类的方法，作用是根据资源路径（如"sms-config.properties"）返回一个「字节输入流」，把classpath下的指定资源文件转换成字节输入流
        try (InputStream is = SmsCodeService.class.getClassLoader()
                .getResourceAsStream("sms-config.properties")) {
            if (is != null) {
                //把字节流的文件内容解析成Properties的键值对
                props.load(is);
                System.out.println("[短信服务] 从 classpath 加载配置");
                return props;
            }
            //Java异常处理「捕获异常但不做任何处理」的特殊写法，用于忽略异常，继续执行后续代码
        } catch (Exception ignored) {}

        return props;
    }

    // ==================== 验证码信息内部类 ====================

    private static class SmsCodeInfo {
        private final String code;
        private final long sendTime;
        private boolean used;

        // 构造函数，初始化验证码和发送时间（将验证码和发送时间封装成一个对象，用于存储）
        SmsCodeInfo(String code, long sendTime) {
            this.code = code;
            this.sendTime = sendTime;
            this.used = false;
        }
    }

    // ==================== 公共 API ====================

    /**
     * 生成6位数字验证码
     */
    //String.format() 是Java 字符串格式化方法，作用是把格式化字符串中的占位符替换为实际值，返回格式化后的字符串（把「数据」按照「指定格式」转换成字符串）
    //%06d 表示「左对齐，不足6位用0填充」，%d 表示「整数」
    //new Random().nextInt(1000000) 是生成一个0到999999之间的随机整数
    public String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }


    /**
     * 发送验证码
     */
    public String sendCode(String phone) {
        //校验手机号是否为空，不能为空，不能为空字符串，不能为空空格
        //trim() 是 String 类的方法，作用是去掉字符串首尾的空格
        //isEmpty() 是 String 类的方法，作用是判断字符串是否为空（包含空格）
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("PHONE_NULL", "手机号不能为空");
        }

        /*
        * 短信验证码「频率限制（限流）」的核心逻辑
        *
        * @param phone 手机号
        * @return 验证码
        * @throws BusinessException 如果手机号为空或无效
        * @throws BusinessException 如果验证码发送过于频繁
        *
        * */
        // 频率限制：每个手机号每60秒最多发送1条验证码，超过60秒后才能重新发送
        SmsCodeInfo existing = SMS_CODE_MAP.get(phone);
        if (existing != null) {
            // 存储计算上次发送时间到当前时间的间隔（毫秒），变量名elapsed是大厂语义化命名规范（Elapsed Time=「经过的时间」）
            //System.currentTimeMillis()是获取当前系统时间的毫秒时间戳
            //existing.sendTime 是上次发送验证码的时间戳
            long elapsed = System.currentTimeMillis() - existing.sendTime;
            // 如果上次发送时间到当前时间的间隔小于60秒，说明发送过于频繁
            if (elapsed < SMS_CODE_EXPIRE_TIME) {
                long seconds = (SMS_CODE_EXPIRE_TIME - elapsed) / 1000;
                throw new BusinessException("SEND_TOO_FAST",
                        "验证码发送过于频繁，请" + seconds + "秒后再试");
            }
        }

        String code = generateCode();

        if (smsEnabled) {
            sendViaAliyun(phone, code);
        } else {
            System.out.println("[短信服务] 📱 模拟发送 → " + maskPhone(phone)
                    + " | 验证码: " + code + " | 原因: " + simulationReason);
        }

        //短信验证码「发送成功后的持久化存储」——把「刚生成的验证码+发送时间」封装成内部类对象，存入线程安全的全局Map，为后续的「验证码校验、频率限制、过期判断」提供核心数据支撑，是短信服务的数据闭环关键步骤
        //new SmsCodeInfo(code, System.currentTimeMillis())封装「验证码+发送时间」为一个对象，用于存储
        SMS_CODE_MAP.put(phone, new SmsCodeInfo(code, System.currentTimeMillis()));
        return code;
    }

    /**
     * 校验验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否校验通过
     * @throws BusinessException 如果手机号为空或无效
     * @throws BusinessException 如果验证码为空或无效
     * @throws BusinessException 如果验证码已过期
     * @throws BusinessException 如果验证码已被使用
     * @throws BusinessException 如果验证码错误
     */

    //定义校验验证码的方法
    public boolean verifyCode(String phone, String code) {
        if (phone == null || phone.trim().isEmpty())
            throw new BusinessException("PHONE_NULL", "手机号不能为空");
        if (code == null || code.trim().isEmpty())
            throw new BusinessException("CODE_NULL", "验证码不能为空");

        SmsCodeInfo info = SMS_CODE_MAP.get(phone);
        if (info == null)
            throw new BusinessException("CODE_NOT_EXIST", "验证码已过期或未发送");
        if (info.used)
            throw new BusinessException("CODE_USED", "验证码已被使用，请重新获取");
        if (System.currentTimeMillis() - info.sendTime > SMS_CODE_EXPIRE_TIME) {
            SMS_CODE_MAP.remove(phone);
            throw new BusinessException("CODE_EXPIRED", "验证码已过期，请重新获取");
        }
        if (!info.code.equals(code))
            throw new BusinessException("CODE_MISMATCH", "验证码错误");

        info.used = true;
        SMS_CODE_MAP.remove(phone);
        return true;
    }

    /**
     * 清除验证码
     * @param phone 手机号
     *
     */
    public void clearCode(String phone) { SMS_CODE_MAP.remove(phone); }

    /**
     * 获取当前验证码
     * @param phone 手机号
     * @return 当前验证码（如果存在）
     */
    public String getCurrentCode(String phone) {
        if (phone == null || phone.trim().isEmpty())
            throw new BusinessException("PHONE_NULL", "手机号不能为空");
        SmsCodeInfo info = SMS_CODE_MAP.get(phone);
        return info != null ? info.code : null;
    }

    // 检查短信服务是否已启用
    public static boolean isSmsEnabled() { return smsEnabled; }
    // 获取模拟发送原因（仅用于测试）
    public static String getSimulationReason() { return simulationReason; }

    // ==================== 阿里云短信 API 调用 ====================

    /**
     * 通过 HTTPS POST 调用阿里云短信 API
     * 签名算法: HMAC-SHA1, 遵循阿里云 POP API 规范
     * @param phone 手机号
     * @param code 验证码
     * @throws Exception 如果发送失败
     * @throws BusinessException 如果手机号为空或无效
     * @throws BusinessException 如果验证码为空或无效
     * @throws BusinessException 如果短信服务未启用
     * @throws BusinessException 如果验证码发送失败
     */
    // 发送验证码到指定手机号
    private void sendViaAliyun(String phone, String code) {
        try {
            // 1. 构建请求参数（不含 Signature）
            Map<String, String> params = new TreeMap<>(); // TreeMap 自动按 key 排序
            params.put("Action", "SendSms");
            params.put("Version", "2017-05-25");
            params.put("RegionId", "cn-hangzhou");
            params.put("PhoneNumbers", phone);
            params.put("SignName", signName);
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", "{\"code\":\"" + code + "\"}");
            params.put("AccessKeyId", accessKeyId);
            params.put("Format", "JSON");
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("Timestamp", formatUTC(new Date()));

            // 2. 计算签名
            String signature = computeSignature(params, "POST");
            params.put("Signature", signature);

            // 3. 构建 POST body
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (body.length() > 0) body.append("&");
                body.append(percentEncode(e.getKey()))
                    .append("=")
                    .append(percentEncode(e.getValue()));
            }

            // 4. 发送 HTTPS 请求
            String response = httpPost(SMS_ENDPOINT, body.toString());

            // 5. 判断结果
            if (response.contains("\"Code\":\"OK\"")) {
                System.out.println("[短信服务] ✅ 短信已发送 → " + maskPhone(phone)
                        + " | 验证码: " + code);
            } else {
                String errMsg = extractJsonField(response, "Message");
                String errCode = extractJsonField(response, "Code");
                System.out.println("[短信服务] ❌ 发送失败 → " + maskPhone(phone)
                        + " | " + errCode + ": " + errMsg);
                throw new BusinessException("SMS_SEND_FAIL",
                        errMsg != null ? errMsg : "短信发送失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("[短信服务] ❌ 网络异常 → " + e.getMessage());
            throw new BusinessException("SMS_API_ERROR", "短信服务异常，请稍后重试");
        }
    }

    /**
     * 计算阿里云 POP API 签名
     *
     * 步骤:
     *   1. 对所有参数按 key 排序 (TreeMap 已保证)
     *   2. 构建 canonicalQueryString = percentEncode(k1)=percentEncode(v1)&...
     *   3. stringToSign = HTTP_METHOD + "&" + percentEncode("/") + "&" + percentEncode(canonicalQueryString)
     *   4. HMAC-SHA1 签名, key = accessKeySecret + "&"
     *   5. Base64 编码
     */

    private String computeSignature(Map<String, String> params, String httpMethod)
            throws Exception {
        // 构建规范化查询字符串
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (canonical.length() > 0) canonical.append("&");
            canonical.append(percentEncode(e.getKey()))
                     .append("=")
                     .append(percentEncode(e.getValue()));
        }

        // 构建待签名字符串
        String stringToSign = httpMethod + "&"
                + percentEncode("/") + "&"
                + percentEncode(canonical.toString());

        // HMAC-SHA1 签名
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8),
                "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signData);
    }

    /**
     * 发送 HTTPS POST 请求
     */
    private String httpPost(String urlStr, String body) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream() : conn.getErrorStream();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * RFC 3986 百分号编码
     * 注意: 阿里云要求 ~ 不编码，空格编码为 %20（不是 +）
     */
    private String percentEncode(String value) throws UnsupportedEncodingException {
        if (value == null) return "";
        return URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    /**
     * 从 JSON 字符串中简单提取字段值（避免引入 JSON 库）
     */
    private String extractJsonField(String json, String field) {
        if (json == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    // ==================== 工具方法 ====================

    /** 格式化 UTC 时间（阿里云 API 要求 ISO 8601 格式） */
    private String formatUTC(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /** 隐藏手机号中间四位（用于显示） */
    //maskPhone用来隐藏手机号中间四位，保护用户隐私
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 检查字符串是否为空或仅包含空格 */
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
