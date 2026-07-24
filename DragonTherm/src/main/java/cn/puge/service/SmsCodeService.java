package cn.puge.service;

import cn.puge.exception.BusinessException;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务
 * 负责验证码的生成、存储和校验
 * 注意：生产环境中应使用Redis存储，这里使用ConcurrentHashMap模拟
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class SmsCodeService {

    /**
     * 验证码有效时间（毫秒）
     * 默认1分钟 = 60秒 = 60000毫秒
     */
    private static final long SMS_CODE_EXPIRE_TIME = 60 * 1000;

    /**
     * 存放验证码的Map集合
     * key: 手机号
     * value: SmsCodeInfo（包含验证码和发送时间）
     */
    private static final Map<String, SmsCodeInfo> SMS_CODE_MAP = new ConcurrentHashMap<String, SmsCodeInfo>();

    /**
     * 验证码信息内部类
     */
    private static class SmsCodeInfo {
        /**
         * 验证码
         */
        private String code;

        /**
         * 发送时间
         */
        private long sendTime;

        /**
         * 验证码状态（是否已使用）
         */
        private boolean used;

        public SmsCodeInfo(String code, long sendTime) {
            this.code = code;
            this.sendTime = sendTime;
            this.used = false;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public long getSendTime() {
            return sendTime;
        }

        public void setSendTime(long sendTime) {
            this.sendTime = sendTime;
        }

        public boolean isUsed() {
            return used;
        }

        public void setUsed(boolean used) {
            this.used = used;
        }
    }

    /**
     * 生成6位数字验证码
     *
     * @return 6位数字字符串
     */
    public String generateCode() {
        Random random = new Random();
        int code = random.nextInt(1000000);
        return String.format("%06d", code);
    }

    /**
     * 发送验证码
     * 将验证码存入缓存并返回（模拟发送，实际应调用短信网关）
     *
     * @param phone 手机号
     * @return 发送的验证码
     * @throws BusinessException 发送失败时抛出
     */
    public String sendCode(String phone) {
        // 参数校验
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("PHONE_NULL", "手机号不能为空");
        }

        // 检查发送频率（60秒内不能重复发送）
        SmsCodeInfo existingCode = SMS_CODE_MAP.get(phone);
        if (existingCode != null) {
            long elapsedTime = System.currentTimeMillis() - existingCode.getSendTime();
            if (elapsedTime < SMS_CODE_EXPIRE_TIME) {
                long remainingSeconds = (SMS_CODE_EXPIRE_TIME - elapsedTime) / 1000;
                throw new BusinessException("SEND_TOO_FAST",
                        "验证码发送过于频繁，请" + remainingSeconds + "秒后再试");
            }
        }

        // 生成新验证码
        String code = generateCode();
        long sendTime = System.currentTimeMillis();

        // 存入缓存
        SmsCodeInfo smsCodeInfo = new SmsCodeInfo(code, sendTime);
        SMS_CODE_MAP.put(phone, smsCodeInfo);

        // 模拟发送短信（实际应调用短信网关API）
        System.out.println("【短信网关模拟】向手机号 " + maskPhone(phone) + " 发送验证码: " + code);

        return code;
    }

    /**
     * 校验验证码
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 校验是否通过
     * @throws BusinessException 校验失败时抛出
     */
    public boolean verifyCode(String phone, String code) {
        // 参数校验
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("PHONE_NULL", "手机号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("CODE_NULL", "验证码不能为空");
        }

        // 获取存储的验证码信息
        SmsCodeInfo smsCodeInfo = SMS_CODE_MAP.get(phone);
        if (smsCodeInfo == null) {
            throw new BusinessException("CODE_NOT_EXIST", "验证码已过期或未发送");
        }

        // 检查验证码是否已使用
        if (smsCodeInfo.isUsed()) {
            throw new BusinessException("CODE_USED", "验证码已被使用，请重新获取");
        }

        // 检查验证码是否过期
        long elapsedTime = System.currentTimeMillis() - smsCodeInfo.getSendTime();
        if (elapsedTime > SMS_CODE_EXPIRE_TIME) {
            // 清除过期验证码
            SMS_CODE_MAP.remove(phone);
            throw new BusinessException("CODE_EXPIRED", "验证码已过期，请重新获取");
        }

        // 校验验证码是否匹配
        if (!smsCodeInfo.getCode().equals(code)) {
            throw new BusinessException("CODE_MISMATCH", "验证码错误");
        }

        // 标记验证码为已使用
        smsCodeInfo.setUsed(true);

        // 清除已使用的验证码
        SMS_CODE_MAP.remove(phone);

        return true;
    }

    /**
     * 手机号脱敏处理
     * 例如：13546069966 -> 135****9966
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 清除用户的所有验证码记录
     * （用于测试或强制刷新）
     *
     * @param phone 手机号
     */
    public void clearCode(String phone) {
        SMS_CODE_MAP.remove(phone);
    }

    /**
     * 获取缓存中当前的验证码信息
     * （仅用于调试）
     *
     * @param phone 手机号
     * @return 验证码信息，未找到返回null
     */
    public String getCurrentCode(String phone) {
        SmsCodeInfo info = SMS_CODE_MAP.get(phone);
        return info != null ? info.getCode() : null;
    }
}
