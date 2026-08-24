package cn.puge.service;

import cn.puge.entity.PugeUser;
import cn.puge.exception.BusinessException;
import cn.puge.request.LoginRequest;
import cn.puge.request.RegisterRequest;
import cn.puge.response.LoginResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录业务服务类
 * 核心业务逻辑，处理三种登录方式
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class LoginService {

    /**
     * 短信验证码服务
     */
    private final SmsCodeService smsCodeService;

    /**
     * 存放用户信息的Map集合
     * 运用静态的Map集合来模拟数据库中的数据
     * key: 手机号（微信用户为 "WECHAT_" + openId）
     * value: 用户信息
     */
    //ConcurrentHashMap是一个Java官方提供的线程安全的Map集合，用于存储用户信息
    //key是手机号，value是用户对象
    //ConcurrentHashMap的线程安全性确保了在多线程环境下并发访问时不会出现数据不一致的情况,单线程使用HashMap
    private static final Map<String, PugeUser> PUGE_USER_MAP = new ConcurrentHashMap<>();

    /**
     * 存放用户ID到用户对象的映射
     * 用于通过token快速查找用户的完整信息（含角色）
     * key: userId
     * value: 用户信息
     */
    private static final Map<String, PugeUser> USER_ID_MAP = new ConcurrentHashMap<>();

    /**
     * 存放AccessToken的Map集合
     * key: token值
     * value: 用户ID
     */
    private static final Map<String, String> TOKEN_USER_MAP = new ConcurrentHashMap<>();

    /**
     * AccessToken有效时间（秒）
     * 默认2小时
     */
    //Token值就是验证用户身份的凭证，用于后续的请求验证
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 2 * 60 * 60;

    /**
     * RefreshToken有效时间（秒）
     * 默认7天
     */
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60;

    /**
     * 静态代码块，初始化模拟用户数据
     */
    static {
        // 模拟用户一（管理员）
        PugeUser pugeUser = new PugeUser();
        pugeUser.setUserId("U001");
        pugeUser.setPhone("13546069966");
        pugeUser.setPassword("123456");
        pugeUser.setNickname("系统管理员一");
        pugeUser.setRole("admin");
        pugeUser.setCreateTime(System.currentTimeMillis());
        //采用多索引分离的设计，在不同的业务场景下使用不同的索引，降低了时间复杂度，提高了查询效率
        //PUGE_USER_MAP是根据手机号查询用户信息的索引，USER_ID_MAP是根据用户ID查询用户信息的索引
        PUGE_USER_MAP.put("13546069966", pugeUser);
        USER_ID_MAP.put("U001", pugeUser);

        // 模拟用户二（普通用户）
        PugeUser pugeUser1 = new PugeUser();
        pugeUser1.setUserId("U002");
        pugeUser1.setPhone("13935193040");
        pugeUser1.setPassword("654321");
        pugeUser1.setNickname("普通用户一");
        pugeUser1.setRole("user");
        pugeUser1.setCreateTime(System.currentTimeMillis());
        PUGE_USER_MAP.put("13935193040", pugeUser1);
        USER_ID_MAP.put("U002", pugeUser1);

        // 模拟微信用户（用于微信登录测试）
        PugeUser wechatUser = new PugeUser();
        wechatUser.setUserId("W001");
        wechatUser.setWechatOpenId("oABC123DEF456");
        wechatUser.setNickname("微信用户");
        //模拟从微信服务器获取的头像URL，实际应用中需要从微信服务器获取
        wechatUser.setAvatarUrl("https://example.com/avatar.jpg");
        wechatUser.setRole("user");
        wechatUser.setCreateTime(System.currentTimeMillis());
        // 使用特殊前缀存储微信用户
        PUGE_USER_MAP.put("WECHAT_oABC123DEF456", wechatUser);
        USER_ID_MAP.put("W001", wechatUser);
    }

    /**
         * 构造方法：初始化LoginService实例并完成核心依赖组件的手动注入
     *
     * 设计说明（大厂规范要求的注释维度）：
     * 1. 语法强制要求：成员变量 {@link #smsCodeService} 被声明为 {@code private final}，
     *    必须在构造方法中完成初始化（否则Java编译器会报错，提示未初始化的final变量）
     * 2. 依赖注入方式：当前项目为纯Java演示工程（未引入Spring等IoC依赖注入框架），
     *    采用【手动依赖初始化】；若后续引入Spring框架，可替换为 {@code @Autowired} 注解实现自动注入
     * 3. 封装性保障：将SmsCodeService实例封装在LoginService内部，外部类（如 {@code dragonThermLogin}）
     *    无法直接访问或修改该组件，保证业务逻辑的封装性
     *
     * 业务作用：
     * 为短信验证码登录、验证码发送/校验等核心业务提供必要的组件支持，
     * 避免运行时出现空指针异常（NullPointerException）
     *
     * @see SmsCodeService#sendCode(String) 验证码发送核心方法
     * @see SmsCodeService#verifyCode(String, String) 验证码校验核心方法
     */
    public LoginService() {
        // 1. new SmsCodeService()：创建短信验证码服务实例（负责验证码生成、存储、校验的核心组件）
        // 2. this.smsCodeService：使用this关键字指代【当前正在创建的LoginService实例】，明确赋值目标，避免变量重名冲突
        // 3. 赋值效果：LoginService实例持有SmsCodeService的引用，后续业务方法（如sendSmsCode()、handleSmsCodeLogin()）
        //    可直接调用该实例，无需重复创建，保证实例唯一性与线程安全
        this.smsCodeService = new SmsCodeService();
    }

    /**
     * 用户登录入口
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    //定义登录方法login，参数为LoginRequest封装的登录请求参数，返回值为LoginResponse封装的登录响应结果
    public LoginResponse login(LoginRequest request) {
        //运用try-catch语句，捕获可能的异常，避免程序崩溃，确保实现【异常隔离+统一响应】
        try {
            // 参数校验
            //调用LoginRequest.java的validate方法，校验登录请求参数是否符合要求，如手机号、密码、登录类型等
            request.validate();

            // 根据登录类型分发处理
            //将登录类型转换为枚举类型，方便后续判断
            LoginRequest.LoginType loginType = LoginRequest.LoginType.getByCode(request.getLoginType());
            if (loginType == null) {
                return LoginResponse.fail(400, "不支持的登录类型");
            }

            switch (loginType) {
                case SMS_CODE:
                    return handleSmsCodeLogin(request);
                case WECHAT:
                    return handleWechatLogin(request);
                case PASSWORD:
                    return handlePasswordLogin(request);
                default:
                    return LoginResponse.fail(400, "登录类型处理异常");
            }
            //捕获业务规则异常，如手机号不存在、密码错误等，返回对应的错误响应
        } catch (BusinessException e) {
            return LoginResponse.fail(400, e.getMessage());
            //捕获参数非法异常，如手机号格式错误、登录类型不存在等，返回对应的错误响应
        } catch (IllegalArgumentException e) {
            return LoginResponse.fail(400, e.getMessage());
            //兜底捕获，捕获所有未知其他异常，如数据库异常、网络异常等，返回对应的错误响应
        } catch (Exception e) {
            return LoginResponse.fail(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 用户注册
     * 注册成功后自动登录，返回包含token的登录响应
     *
     * @param request 注册请求参数
     * @return 登录响应（包含token和用户信息）
     */
    public LoginResponse register(RegisterRequest request) {
        try {
            // 参数校验
            request.validate();
            //调用RegisterRequest DTO的getter方法从注册请求参数中获取手机号
            String phone = request.getPhone();

            // 检查手机号是否已注册
            //containsKey 是 Java Map 接口的内置方法，作用是判断Map集合中是否存在指定的「键（key）」
            if (PUGE_USER_MAP.containsKey(phone)) {
                return LoginResponse.fail(400, "该手机号已注册");
            }

            // 创建新用户
            PugeUser user = registerUser(phone, request.getPassword(), null);

            // 如果用户提供了昵称，覆盖默认昵称
            if (request.getNickname() != null) {
                user.setNickname(request.getNickname());
            }

            // 生成令牌并返回（注册即登录）
            //调用LoginService.java的generateLoginResponse方法，生成包含token和用户信息的登录响应
            LoginResponse response = generateLoginResponse(user);
            response.setMessage("注册成功");
            return response;
            //捕获业务规则异常，如手机号格式错误、密码格式错误等，返回对应的错误响应
        } catch (BusinessException e) {
            return LoginResponse.fail(400, e.getMessage());
            //捕获参数非法异常，如手机号格式错误、密码格式错误等，返回对应的错误响应
        } catch (IllegalArgumentException e) {
            return LoginResponse.fail(400, e.getMessage());
            //兜底捕获，捕获所有未知其他异常，如数据库异常、网络异常等，返回对应的错误响应
        } catch (Exception e) {
            return LoginResponse.fail(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 处理手机号验证码登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private LoginResponse handleSmsCodeLogin(LoginRequest request) {
        String phone = request.getPhone();
        String smsCode = request.getSmsCode();

        // 校验验证码
        //调用SmsCodeService.java的verifyCode方法，校验手机号和验证码是否匹配
        smsCodeService.verifyCode(phone, smsCode);

        // 查询用户（不存在则自动注册）
        PugeUser user = getUserByPhone(phone);
        if (user == null) {
            user = registerUser(phone, null, null);
        }

        // 生成token令牌并返回
        return generateLoginResponse(user);
    }

    /**
     * 处理微信登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private LoginResponse handleWechatLogin(LoginRequest request) {
        String wechatOpenId;

        // 如果请求中已有OpenId，直接使用
        //使用&&是为了校验OpenId非空且非空白字符串
        if (request.getWechatOpenId() != null && !request.getWechatOpenId().trim().isEmpty()) {
            wechatOpenId = request.getWechatOpenId();
        } else {
            // 否则通过授权码换取OpenId（模拟实现）
            wechatOpenId = exchangeCodeForOpenId(request.getWechatCode());
        }

        // 查询微信用户
        PugeUser user = getUserByWechatOpenId(wechatOpenId);
        if (user == null) {
            // 模拟微信授权获取用户信息
            user = registerWechatUser(wechatOpenId);
        }

        // 生成token令牌并返回
        return generateLoginResponse(user);
    }

    /**
     * 处理账号密码登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private LoginResponse handlePasswordLogin(LoginRequest request) {
        String phone = request.getPhone();
        String password = request.getPassword();

        // 查询用户
        PugeUser user = getUserByPhone(phone);
        if (user == null) {
            return LoginResponse.fail(400, "用户不存在");
        }

        // 校验密码
        if (!password.equals(user.getPassword())) {
            return LoginResponse.fail(400, "密码错误");
        }

        // 生成token令牌并返回
        return generateLoginResponse(user);
    }

    /**
     * 生成登录响应
     *
     * @param user 用户信息
     * @return 登录响应
     */
    //把「已验证的用户实体」转换成「带Token的登录响应」，是登录流程的最后一步
    //generateLoginResponse方法的作用是「Entity → 登录响应DTO的转换」，将用户实体转换为登录响应DTO
    //该方法属于内部业务逻辑，不能直接暴露给外部调用，故设为private访问权限
    //体现了分层架构+数据对象隔离的核心思想，将业务逻辑与数据访问逻辑分离，提高了系统的可维护性和可扩展性
    private LoginResponse generateLoginResponse(PugeUser user) {
        /*
        * 采用大厂「双Token机制」——平衡「安全性」和「用户体验」的标准设计，两个Token各司其职，缺一不可
        * 在accessToken（访问令牌）过期时需要使用refreshToken（刷新令牌）获取新的accessToken，确保用户登录状态不中断
        * refreshToken（刷新令牌）过期时需要重新登录，确保用户账号安全
        * */
        // 调用generateToken方法，生成token并赋值给变量accessToken，特点：生存周期短
        String accessToken = generateToken();
        // 调用generateToken方法，生成refreshtoken并赋值给变量refreshToken，特点：生存周期长
        String refreshToken = generateToken();

        // 存储Token映射
        //Map.put(K, V)是Java Map接口的内置方法，作用是把「键-值对」存入Map集合中，键是accessToken，值是用户ID
        //TOKEN_USER_MAP是定义的全局线程安全Map（来自LoginService.java的静态变量TOKEN_USER_MAP）用于存储accessToken和用户ID的映射关系
        //确保在多线程环境下，能够安全地获取和更新用户登录状态
        //同时，也避免了重复登录导致的登录状态冲突问题
        TOKEN_USER_MAP.put(accessToken, user.getUserId());

        // 返回登录响应DTO
        //包含用户信息、accessToken、refreshToken、accessToken过期时间
        return LoginResponse.success(user, accessToken, refreshToken, ACCESS_TOKEN_EXPIRE_TIME);
    }

    /**
     * 生成随机令牌
     *
     * @return UUID格式的令牌
     */
    //Java 生成全局唯一随机令牌（Token）的标准写法，由3个链式调用组成
    //UUID.randomUUID()：生成随机唯一标识符的静态方法，UUID 是 Java 内置的工具类（java.util.UUID），专门用来生成「全局唯一标识符」
    //randomUUID() 是 UUID 类的静态方法（与之前的ValidationUtil.isValidPhone()一样，用static修饰，无需创建实例即可调用），作用是生成一个基于随机数的唯一UUID实例
    //randomUUID底层用的是SecureRandom类（安全随机数生成器），而不是普通的Random
    //普通的random是伪随机可预测（黑客可以通过之前的随机数猜测下一个），SecureRandom是强随机，不可预测（基于硬件噪声，系统熵源，黑客无法预测）
    //toString()：将UUID对象转换成默认格式的字符串表示
    //.replace("-", "")是Java String类的实例方法，作用是把字符串中所有的「横线（-）」替换成「空字符串」，从而得到一个没有横线的UUID字符串
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息，不存在返回null
     */
    private PugeUser getUserByPhone(String phone) {
        return PUGE_USER_MAP.get(phone);
    }

    /**
     * 根据微信OpenId查询用户
     *
     * @param wechatOpenId 微信OpenId
     * @return 用户信息，不存在返回null
     */
    private PugeUser getUserByWechatOpenId(String wechatOpenId) {
        return PUGE_USER_MAP.get("WECHAT_" + wechatOpenId);
    }

    /**
     * 注册新用户（手机号注册）
     *
     * @param phone        手机号
     * @param password     密码（可为null）
     * @param wechatOpenId 微信OpenId（可为null）
     * @return 注册的用户信息
     */
    private PugeUser registerUser(String phone, String password, String wechatOpenId) {
        PugeUser user = new PugeUser();
        //在用户id里添加前缀U，是为了表示用户类型为普通用户，后面添加当前时间戳，是为了确保用户id的唯一性
        user.setUserId("U" + System.currentTimeMillis());
        user.setPhone(phone);
        user.setPassword(password);
        user.setWechatOpenId(wechatOpenId);
        //phone.length() - 4是为了取手机号的后4位（计算后四位的起始索引），作为昵称的后缀，确保昵称的唯一性
        //phone.substring从索引phone.length() - 4开始取，取后4位
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        //默认角色为普通user
        user.setRole("user");
        user.setCreateTime(System.currentTimeMillis());

        //将新注册的用户同时存入PUGE_USER_MAP和USER_ID_MAP，确保在多线程环境下能够安全地获取和更新用户信息
        //同时存入PUGE_USER_MAP是为了根据手机号查询用户，USER_ID_MAP是为了根据用户ID查询用户，确保在多线程环境下能够安全地获取和更新用户信息
        //put同时存入两个索引map，实现多索引的构建
        PUGE_USER_MAP.put(phone, user);
        USER_ID_MAP.put(user.getUserId(), user);
        return user;
    }

    /**
     * 注册微信用户（模拟）
     *
     * @param wechatOpenId 微信OpenId
     * @return 注册的用户信息
     */
    private PugeUser registerWechatUser(String wechatOpenId) {
        PugeUser user = new PugeUser();
        //在用户id里添加前缀W，是为了表示用户类型为微信用户，增加类型辨识度，后面添加当前时间戳，是为了确保用户id的唯一性
        user.setUserId("W" + System.currentTimeMillis());
        user.setWechatOpenId(wechatOpenId);
        user.setNickname("微信用户" + wechatOpenId.substring(wechatOpenId.length() - 4));
        user.setAvatarUrl("https://example.com/default-avatar.jpg");
        user.setRole("user");
        user.setCreateTime(System.currentTimeMillis());

        //将新注册的用户同时存入PUGE_USER_MAP和USER_ID_MAP，确保在多线程环境下能够安全地获取和更新用户信息
        PUGE_USER_MAP.put("WECHAT_" + wechatOpenId, user);
        USER_ID_MAP.put(user.getUserId(), user);
        return user;
    }

    /**
     * 模拟微信授权码换取OpenId
     * 生产环境中应调用微信API
     *
     * @param code 微信授权码
     * @return OpenId
     */
    private String exchangeCodeForOpenId(String code) {
        // 模拟实现：实际应调用微信接口
        // 这里简单模拟返回一个OpenId
        System.out.println("【微信授权模拟】通过授权码 " + code + " 换取OpenId");
        return "oSIMPLE" + code.hashCode() % 1000000;
    }

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @return 发送的验证码
     */
    public String sendSmsCode(String phone) {
        return smsCodeService.sendCode(phone);
    }

    /**
     * 验证Token是否有效
     *
     * @param token 访问令牌
     * @return 是否有效
     */
    //TOKEN_USER_MAP 是存储Token→UserId映射的全局Map（键=Token，值=UserId）
    //containsKey方法用于检查Token是否在映射中，即是否有效【鉴权校验】
    public boolean validateToken(String token) {
        return TOKEN_USER_MAP.containsKey(token);
    }

    /**
     * 根据Token获取用户ID
     *
     * @param token 访问令牌
     * @return 用户ID，不存在返回null
     */
    //存放AccessToken的Map集合 key: token值 value: 用户ID
    public String getUserIdByToken(String token) {
        return TOKEN_USER_MAP.get(token);
    }

    /**
     * 获取用户总数（用于测试）
     *
     * @return 用户数量
     */
    //size() 是 Java Map 接口的内置方法，返回 Map 中「键值对的数量」（返回值是 int 类型）：用户总数
    //PUGE_USER_MAP 是存储用户信息的全局Map（键=手机号，值=用户对象）
    //size() 方法用于获取 PUGE_USER_MAP 中键值对的数量，即用户总数
    public int getUserCount() {
        return PUGE_USER_MAP.size();
    }

    // ==================== 管理员功能 ====================

    /**
     * 根据Token获取完整用户信息（含角色）
     *
     * @param token 访问令牌
     * @return 用户信息，不存在返回null
     */
    public PugeUser getUserByToken(String token) {
        String userId = TOKEN_USER_MAP.get(token);
        if (userId == null) {
            return null;
        }
        return USER_ID_MAP.get(userId);
    }

    /**
     * 校验Token是否为管理员
     *
     * @param token 访问令牌
     * @return 是否为管理员
     */
    public boolean isAdmin(String token) {
        PugeUser user = getUserByToken(token);
        return user != null && "admin".equals(user.getRole());
    }

    /**
     * 获取所有用户列表（仅管理员可用）
     *
     * @return 所有用户的集合
     */
    //Collection<PugeUser> 是「存储PugeUser类型的集合接口」，是大厂「依赖倒置+类型安全」规范的典型体现
    public java.util.Collection<PugeUser> getAllUsers() {
        return PUGE_USER_MAP.values();
    }

    /**
     * 管理员删除用户
     *
     * @param adminToken 管理员令牌
     * @param phone      要删除的用户手机号
     * @return 操作结果消息
     * @throws IllegalArgumentException 权限不足或操作不合法时抛出
     */
    public String deleteUser(String adminToken, String phone) {
        // 校验管理员权限
        PugeUser adminUser = getUserByToken(adminToken);
        if (adminUser == null) {
            throw new IllegalArgumentException("未登录或令牌无效");
        }
        if (!"admin".equals(adminUser.getRole())) {
            throw new IllegalArgumentException("无管理员权限");
        }

        // 查找要删除的用户
        PugeUser targetUser = PUGE_USER_MAP.get(phone);
        if (targetUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 不能删除自己
        if (adminUser.getUserId().equals(targetUser.getUserId())) {
            throw new IllegalArgumentException("不能删除自己的账号");
        }

        // 不能删除其他管理员
        if ("admin".equals(targetUser.getRole())) {
            throw new IllegalArgumentException("不能删除其他管理员账号");
        }

        // 从数据存储中移除
        PUGE_USER_MAP.remove(phone);
        USER_ID_MAP.remove(targetUser.getUserId());

        //Java 集合框架（Map/List/Set）统一用 remove 方法，没有 delete 方法——因为集合是「内存中的容器」，从容器中取走元素是「移除」（remove），而不是「销毁」（delete）。

        // 清理该用户的所有token（遍历TOKEN_USER_MAP）
        //TOKEN_USER_MAP 是存储Token→UserId映射的全局Map（键=Token，值=UserId）
        //entrySet是Java Map 接口的内置方法，返回Map中所有键值对（Entry）的集合，每个Entry包含一个键（Key）和一个值（Value）。
        //entrySet() 方法用于获取 Map 中「键值对集合」，并遍历删除所有与目标用户ID匹配的键值对
        //entry -> targetUser.getUserId().equals(entry.getValue())Lambda表达式（条件判断），如果键值对的value等于目标用户的userId，就删除该键值对
        TOKEN_USER_MAP.entrySet().removeIf(
                entry -> targetUser.getUserId().equals(entry.getValue()));

        System.out.println("【管理员操作】" + adminUser.getNickname()
                + " 删除了用户 " + targetUser.getNickname()
                + " (" + targetUser.getPhone() + ")");
        return "用户 " + targetUser.getNickname() + " 已删除";
    }
}