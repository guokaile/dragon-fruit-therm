(function () {
    'use strict';

    // ============== 环境检测 ==============
    // GitHub Pages 上无法运行 Java 后端，自动切换为演示模式
    var isProduction = window.location.hostname !== 'localhost'
                    && window.location.hostname !== '127.0.0.1';
    console.log('当前环境:', isProduction ? '演示模式 (GitHub Pages)' : '本地模式 (Java后端)');

    // ============== 登录类型常量 ==============
    var LOGIN_TYPE_PASSWORD = 3;   // 账号密码登录
    var LOGIN_TYPE_SMS = 1;        // 手机号验证码登录
    var LOGIN_TYPE_WECHAT = 2;     // 微信登录

    // ============== 演示模式模拟用户数据（与后端 LoginService 一致） ==============
    var MOCK_USERS = {
        '13546069966': {
            userId: 'U001',
            phone: '13546069966',
            password: '123456',
            nickname: '火龙果用户一',
            role: 'admin',
            avatarUrl: ''
        },
        '13935193040': {
            userId: 'U002',
            phone: '13935193040',
            password: '654321',
            nickname: '火龙果用户二',
            role: 'user',
            avatarUrl: ''
        }
    };
    // 微信用户
    var MOCK_WECHAT_USERS = {
        'oABC123DEF456': {
            userId: 'W001',
            wechatOpenId: 'oABC123DEF456',
            nickname: '微信用户',
            role: 'user',
            avatarUrl: 'https://example.com/avatar.jpg'
        }
    };
    // 演示模式下暂存已发送的短信验证码
    var MOCK_SMS_CODES = {};

    // ============== DOM 元素 ==============
    var $tabBtns = document.querySelectorAll('.tab-btn');
    var $tabContents = document.querySelectorAll('.tab-content');
    var $toast = document.getElementById('toast');
    var $loading = document.getElementById('loading');
    var $loginForm = document.getElementById('loginForm');

    var $phone1 = document.getElementById('phone1');
    var $password = document.getElementById('password');
    var $togglePwd = document.getElementById('togglePwd');

    var $phone2 = document.getElementById('phone2');
    var $smsCode = document.getElementById('smsCode');
    var $sendSmsBtn = document.getElementById('sendSmsBtn');

    var $wechatCode = document.getElementById('wechatCode');

    var $regPhone = document.getElementById('regPhone');
    var $regPassword = document.getElementById('regPassword');
    var $regTogglePwd = document.getElementById('regTogglePwd');
    var $regConfirmPassword = document.getElementById('regConfirmPassword');
    var $regConfirmTogglePwd = document.getElementById('regConfirmTogglePwd');
    var $regNickname = document.getElementById('regNickname');

    var $phone1Error = document.getElementById('phone1Error');
    var $passwordError = document.getElementById('passwordError');
    var $phone2Error = document.getElementById('phone2Error');
    var $smsCodeError = document.getElementById('smsCodeError');

    var $regPhoneError = document.getElementById('regPhoneError');
    var $regPasswordError = document.getElementById('regPasswordError');
    var $regConfirmPasswordError = document.getElementById('regConfirmPasswordError');
    var $regNicknameError = document.getElementById('regNicknameError');

    // 当前激活的 tab
    var activeTab = 'password';
    // 倒计时
    var smsCountdown = 0;
    var smsTimer = null;

    // ============== 工具函数 ==============

    function showToast(msg, type, duration) {
        try {
            $toast.textContent = msg;
            $toast.className = 'toast show ' + (type || '');
            clearTimeout($toast._timer);
            $toast._timer = setTimeout(function () {
                $toast.className = 'toast';
            }, duration || 2500);
        } catch (e) {
            alert(msg);
        }
    }

    function showLoading(show) {
        if (show) {
            $loading.classList.add('show');
        } else {
            $loading.classList.remove('show');
        }
    }

    function isPhoneValid(phone) {
        return /^1[3-9]\d{9}$/.test(phone);
    }

    function setFieldError($input, $errorEl, msg) {
        if (!$input || !$errorEl) return;
        if (msg) {
            $input.classList.add('error');
            $errorEl.textContent = msg;
        } else {
            $input.classList.remove('error');
            $errorEl.textContent = '';
        }
    }

    // 发送 HTTP 请求（演示模式下自动模拟）
    function request(url, method, data) {
        return new Promise(function (resolve, reject) {
            // ===== 演示模式：模拟后端响应 =====
            if (isProduction) {
                // 模拟网络延迟
                var delay = 500 + Math.random() * 800;

                if (url === '/api/sendSms') {
                    setTimeout(function () {
                        var payload = (typeof data === 'string') ? JSON.parse(data || '{}') : (data || {});
                        if (!payload.phone || !/^1[3-9]\d{9}$/.test(payload.phone)) {
                            reject({ message: '手机号格式不正确' });
                            return;
                        }
                        // 生成6位模拟验证码
                        var mockCode = String(Math.floor(100000 + Math.random() * 900000));
                        // 存储验证码，用于后续登录校验
                        MOCK_SMS_CODES[payload.phone] = mockCode;
                        console.log('📱 模拟验证码（演示模式）:', mockCode);
                        resolve({
                            success: true,
                            code: 200,
                            message: '验证码已发送（演示模式）',
                            data: mockCode
                        });
                    }, delay);
                    return;
                }

                if (url === '/api/login') {
                    setTimeout(function () {
                        var payload = (typeof data === 'string') ? JSON.parse(data || '{}') : (data || {});
                        var user = null;

                        // ===== 账号密码登录 =====
                        if (payload.loginType === LOGIN_TYPE_PASSWORD) {
                            if (!payload.phone || !payload.password) {
                                reject({ message: '手机号和密码不能为空' });
                                return;
                            }
                            if (!/^1[3-9]\d{9}$/.test(payload.phone)) {
                                reject({ message: '手机号格式不正确' });
                                return;
                            }
                            user = MOCK_USERS[payload.phone];
                            if (!user) {
                                reject({ message: '用户不存在' });
                                return;
                            }
                            if (user.password !== payload.password) {
                                reject({ message: '密码错误' });
                                return;
                            }
                        }

                        // ===== 短信验证码登录 =====
                        if (payload.loginType === LOGIN_TYPE_SMS) {
                            if (!payload.phone) {
                                reject({ message: '手机号不能为空' });
                                return;
                            }
                            if (!/^1[3-9]\d{9}$/.test(payload.phone)) {
                                reject({ message: '手机号格式不正确' });
                                return;
                            }
                            if (!payload.smsCode) {
                                reject({ message: '验证码不能为空' });
                                return;
                            }
                            // 校验验证码
                            var sentCode = MOCK_SMS_CODES[payload.phone];
                            if (!sentCode) {
                                reject({ message: '请先获取验证码' });
                                return;
                            }
                            if (sentCode !== payload.smsCode) {
                                reject({ message: '验证码错误' });
                                return;
                            }
                            // 验证码用完后清除
                            delete MOCK_SMS_CODES[payload.phone];
                            // 短信登录：用户不存在则自动注册
                            user = MOCK_USERS[payload.phone];
                            if (!user) {
                                user = {
                                    userId: 'U' + Date.now(),
                                    phone: payload.phone,
                                    password: '',
                                    nickname: '用户' + payload.phone.slice(-4),
                                    role: 'user',
                                    avatarUrl: ''
                                };
                                MOCK_USERS[payload.phone] = user;
                            }
                        }

                        // ===== 微信登录 =====
                        if (payload.loginType === LOGIN_TYPE_WECHAT) {
                            var wechatId = payload.wechatOpenId || payload.wechatCode || '';
                            if (!wechatId) {
                                reject({ message: '请输入微信授权信息' });
                                return;
                            }
                            // 查找微信用户
                            user = MOCK_WECHAT_USERS[wechatId];
                            if (!user) {
                                // 查找 WECHAT_ 前缀的key
                                var wechatKey = 'WECHAT_' + wechatId;
                                if (MOCK_USERS[wechatKey]) {
                                    user = MOCK_USERS[wechatKey];
                                }
                            }
                            if (!user) {
                                // 微信用户不存在则自动注册
                                user = {
                                    userId: 'W' + Date.now(),
                                    wechatOpenId: wechatId,
                                    nickname: '微信用户' + wechatId.slice(-4),
                                    role: 'user',
                                    avatarUrl: ''
                                };
                                MOCK_WECHAT_USERS[wechatId] = user;
                            }
                        }

                        // ===== 登录成功 =====
                        console.log('✅ 登录成功（演示模式）', user.nickname);
                        resolve({
                            success: true,
                            code: 200,
                            message: '登录成功',
                            accessToken: 'demo_token_' + Date.now(),
                            expiresIn: 7200,
                            userInfo: {
                                userId: user.userId,
                                phone: user.phone || payload.phone || '',
                                nickname: user.nickname || '用户',
                                avatarUrl: user.avatarUrl || '',
                                role: user.role || 'user'
                            }
                        });
                    }, delay);
                    return;
                }

                // ===== 演示模式：注册接口 =====
                if (url === '/api/register') {
                    setTimeout(function () {
                        var payload = (typeof data === 'string') ? JSON.parse(data || '{}') : (data || {});

                        if (!payload.phone || !/^1[3-9]\d{9}$/.test(payload.phone)) {
                            reject({ message: '请输入正确的11位手机号' });
                            return;
                        }
                        if (!payload.password || !/^[a-zA-Z0-9]{6,20}$/.test(payload.password)) {
                            reject({ message: '密码需为6-20位字母或数字' });
                            return;
                        }
                        if (payload.password !== payload.confirmPassword) {
                            reject({ message: '两次密码输入不一致' });
                            return;
                        }
                        // 检查手机号是否已注册
                        if (MOCK_USERS[payload.phone]) {
                            reject({ message: '该手机号已注册' });
                            return;
                        }

                        // 创建新用户
                        var newUser = {
                            userId: 'U' + Date.now(),
                            phone: payload.phone,
                            password: payload.password,
                            nickname: payload.nickname || '用户' + payload.phone.slice(-4),
                            role: 'user',
                            avatarUrl: ''
                        };
                        MOCK_USERS[payload.phone] = newUser;

                        // 持久化到localStorage供管理员面板使用
                        try {
                            var demoUsers = localStorage.getItem('dragon_therm_demo_users');
                            var usersArr = demoUsers ? JSON.parse(demoUsers) : [];
                            // 避免重复
                            usersArr = usersArr.filter(function(u) { return u.phone !== newUser.phone; });
                            usersArr.push({
                                userId: newUser.userId,
                                phone: newUser.phone,
                                password: newUser.password,
                                nickname: newUser.nickname,
                                role: 'user',
                                createTime: Date.now()
                            });
                            localStorage.setItem('dragon_therm_demo_users', JSON.stringify(usersArr));
                        } catch(e) {}

                        console.log('✅ 注册成功（演示模式）', newUser.nickname);
                        resolve({
                            success: true,
                            code: 200,
                            message: '注册成功',
                            accessToken: 'demo_token_' + Date.now(),
                            expiresIn: 7200,
                            userInfo: {
                                userId: newUser.userId,
                                phone: newUser.phone,
                                nickname: newUser.nickname,
                                avatarUrl: newUser.avatarUrl,
                                role: 'user'
                            }
                        });
                    }, delay);
                    return;
                }

                // 未知接口
                setTimeout(function () {
                    reject({ message: '演示模式不支持该接口' });
                }, 100);
                return;
            }

            // ===== 本地模式：真实请求 Java 后端 =====
            try {
                var xhr = new XMLHttpRequest();
                xhr.open(method || 'POST', url, true);
                xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
                xhr.onreadystatechange = function () {
                    if (xhr.readyState === 4) {
                        try {
                            var result = JSON.parse(xhr.responseText);
                            if (xhr.status >= 200 && xhr.status < 300) {
                                resolve(result);
                            } else {
                                reject(result || { message: '请求失败' });
                            }
                        } catch (err) {
                            reject({ message: '服务器响应异常' });
                        }
                    }
                };
                xhr.onerror = function () {
                    reject({ message: '网络连接失败' });
                };
                xhr.timeout = 15000;
                xhr.ontimeout = function () {
                    reject({ message: '请求超时' });
                };
                xhr.send(data ? JSON.stringify(data) : '');
            } catch (err) {
                reject({ message: '请求出错: ' + err.message });
            }
        });
    }

    // 存储登录信息
    function saveLogin(data) {
        try {
            var loginInfo = {
                token: data.accessToken,
                userInfo: {
                    userId: data.userInfo.userId,
                    phone: data.userInfo.phone,
                    nickname: data.userInfo.nickname,
                    avatarUrl: data.userInfo.avatarUrl || '',
                    role: data.userInfo.role || 'user'
                },
                expiresAt: new Date().getTime() + (data.expiresIn || 7200) * 1000
            };
            localStorage.setItem('dragon_therm_login', JSON.stringify(loginInfo));
        } catch (e) {
            // localStorage 不可用时忽略
        }
    }

    // ============== Tab 切换 ==============

    function switchTab(tab) {
        activeTab = tab;

        $tabBtns.forEach(function (btn) {
            if (btn.getAttribute('data-tab') === tab) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        $tabContents.forEach(function (content) {
            if (content.getAttribute('data-content') === tab) {
                content.classList.add('active');
            } else {
                content.classList.remove('active');
            }
        });

        // 清除所有错误提示
        setFieldError($phone1, $phone1Error, '');
        setFieldError($password, $passwordError, '');
        setFieldError($phone2, $phone2Error, '');
        setFieldError($smsCode, $smsCodeError, '');
        setFieldError($regPhone, $regPhoneError, '');
        setFieldError($regPassword, $regPasswordError, '');
        setFieldError($regConfirmPassword, $regConfirmPasswordError, '');
        setFieldError($regNickname, $regNicknameError, '');
    }

    $tabBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
            var tab = btn.getAttribute('data-tab');
            switchTab(tab);
        });
    });

    // ============== 密码显示/隐藏 ==============

    $togglePwd.addEventListener('click', function () {
        var isPwd = $password.type === 'password';
        $password.type = isPwd ? 'text' : 'password';
        $togglePwd.textContent = isPwd ? '🙈' : '👁';
    });

    // 注册-密码显示/隐藏
    if ($regTogglePwd) {
        $regTogglePwd.addEventListener('click', function () {
            var isPwd = $regPassword.type === 'password';
            $regPassword.type = isPwd ? 'text' : 'password';
            $regTogglePwd.textContent = isPwd ? '🙈' : '👁';
        });
    }

    // 注册-确认密码显示/隐藏
    if ($regConfirmTogglePwd) {
        $regConfirmTogglePwd.addEventListener('click', function () {
            var isPwd = $regConfirmPassword.type === 'password';
            $regConfirmPassword.type = isPwd ? 'text' : 'password';
            $regConfirmTogglePwd.textContent = isPwd ? '🙈' : '👁';
        });
    }

    // ============== 短信验证码倒计时 ==============

    function startCountdown() {
        smsCountdown = 60;
        $sendSmsBtn.disabled = true;
        $sendSmsBtn.textContent = smsCountdown + 's 后重试';
        smsTimer = setInterval(function () {
            smsCountdown--;
            if (smsCountdown <= 0) {
                clearInterval(smsTimer);
                $sendSmsBtn.disabled = false;
                $sendSmsBtn.textContent = '获取验证码';
            } else {
                $sendSmsBtn.textContent = smsCountdown + 's 后重试';
            }
        }, 1000);
    }

    // 发送短信验证码
    $sendSmsBtn.addEventListener('click', function () {
        var phone = $phone2.value.trim();
        setFieldError($phone2, $phone2Error, '');

        if (!phone) {
            setFieldError($phone2, $phone2Error, '请输入手机号');
            $phone2.focus();
            return;
        }
        if (!isPhoneValid(phone)) {
            setFieldError($phone2, $phone2Error, '手机号格式不正确');
            $phone2.focus();
            return;
        }

        $sendSmsBtn.disabled = true;
        $sendSmsBtn.textContent = '发送中...';

        request('/api/sendSms', 'POST', { phone: phone })
            .then(function (res) {
                if (res.success) {
                    showToast('验证码发送成功：' + (res.data || ''), 'success');
                    startCountdown();
                } else {
                    $sendSmsBtn.disabled = false;
                    $sendSmsBtn.textContent = '获取验证码';
                    showToast(res.message || '发送失败', 'error');
                }
            })
            .catch(function (err) {
                $sendSmsBtn.disabled = false;
                $sendSmsBtn.textContent = '获取验证码';
                showToast(err.message || '发送失败', 'error');
            });
    });

    // ============== 构建登录请求 ==============

    function buildPayload() {
        var payload = {};

        if (activeTab === 'password') {
            var phone = $phone1.value.trim();
            var pwd = $password.value;

            if (!phone) {
                setFieldError($phone1, $phone1Error, '请输入手机号');
                $phone1.focus();
                return null;
            }
            if (!isPhoneValid(phone)) {
                setFieldError($phone1, $phone1Error, '手机号格式不正确');
                $phone1.focus();
                return null;
            }
            if (!pwd) {
                setFieldError($password, $passwordError, '请输入密码');
                $password.focus();
                return null;
            }

            payload.loginType = LOGIN_TYPE_PASSWORD;
            payload.phone = phone;
            payload.password = pwd;
            return payload;
        }

        if (activeTab === 'sms') {
            var phone2 = $phone2.value.trim();
            var code = $smsCode.value.trim();

            if (!phone2) {
                setFieldError($phone2, $phone2Error, '请输入手机号');
                $phone2.focus();
                return null;
            }
            if (!isPhoneValid(phone2)) {
                setFieldError($phone2, $phone2Error, '手机号格式不正确');
                $phone2.focus();
                return null;
            }
            if (!code) {
                setFieldError($smsCode, $smsCodeError, '请输入验证码');
                $smsCode.focus();
                return null;
            }
            if (!/^\d{6}$/.test(code)) {
                setFieldError($smsCode, $smsCodeError, '验证码必须为6位数字');
                $smsCode.focus();
                return null;
            }

            payload.loginType = LOGIN_TYPE_SMS;
            payload.phone = phone2;
            payload.smsCode = code;
            return payload;
        }

        if (activeTab === 'wechat') {
            var wechatVal = ($wechatCode.value || '').trim();
            if (!wechatVal) {
                showToast('请输入微信授权码或OpenId', 'error');
                $wechatCode.focus();
                return null;
            }
            // 如果包含 openid / OpenId 字样则作为 openId 发送，否则作为 code 发送
            if (/openid|OpenId|open_id/i.test(wechatVal)) {
                payload.loginType = LOGIN_TYPE_WECHAT;
                payload.wechatOpenId = wechatVal;
            } else {
                payload.loginType = LOGIN_TYPE_WECHAT;
                payload.wechatCode = wechatVal;
            }
            return payload;
        }

        if (activeTab === 'register') {
            var regPhoneVal = ($regPhone.value || '').trim();
            var regPwd = $regPassword.value;
            var regConfirmPwd = $regConfirmPassword.value;
            var regNick = ($regNickname.value || '').trim();

            // 手机号校验
            if (!regPhoneVal) {
                setFieldError($regPhone, $regPhoneError, '请输入手机号');
                $regPhone.focus();
                return null;
            }
            if (!isPhoneValid(regPhoneVal)) {
                setFieldError($regPhone, $regPhoneError, '请输入正确的11位手机号');
                $regPhone.focus();
                return null;
            }

            // 密码校验
            if (!regPwd) {
                setFieldError($regPassword, $regPasswordError, '请输入密码');
                $regPassword.focus();
                return null;
            }
            if (!/^[a-zA-Z0-9]{6,20}$/.test(regPwd)) {
                setFieldError($regPassword, $regPasswordError, '密码需为6-20位字母或数字');
                $regPassword.focus();
                return null;
            }

            // 确认密码校验
            if (!regConfirmPwd) {
                setFieldError($regConfirmPassword, $regConfirmPasswordError, '请确认密码');
                $regConfirmPassword.focus();
                return null;
            }
            if (regPwd !== regConfirmPwd) {
                setFieldError($regConfirmPassword, $regConfirmPasswordError, '两次密码输入不一致');
                $regConfirmPassword.focus();
                return null;
            }

            // 昵称校验（可选）
            if (regNick && regNick.length > 20) {
                setFieldError($regNickname, $regNicknameError, '昵称不能超过20个字符');
                $regNickname.focus();
                return null;
            }

            payload.phone = regPhoneVal;
            payload.password = regPwd;
            payload.confirmPassword = regConfirmPwd;
            if (regNick) {
                payload.nickname = regNick;
            }
            return payload;
        }

        return null;
    }

    // ============== 执行登录 ==============

    function doLogin() {
        var payload = buildPayload();
        if (!payload) return;

        showLoading(true);

        // 注册走注册接口，登录走登录接口
        var apiUrl = (activeTab === 'register') ? '/api/register' : '/api/login';

        request(apiUrl, 'POST', payload)
            .then(function (res) {
                showLoading(false);
                if (res.success) {
                    saveLogin(res);
                    var nickname = (res.userInfo && res.userInfo.nickname) || '用户';
                    var successMsg = (activeTab === 'register') ? '注册成功，欢迎 ' + nickname : '登录成功，欢迎 ' + nickname;
                    showToast(successMsg, 'success', 3000);
                    // 1.5秒后跳转到主页
                    setTimeout(function() {
                        window.location.href = 'index.html';
                    }, 1500);
                } else {
                    showToast(res.message || '登录失败', 'error');
                }
            })
            .catch(function (err) {
                showLoading(false);
                showToast(err.message || '登录失败，请重试', 'error');
            });
    }

    // 统一处理表单提交 - 根据按钮的 data-tab 决定当前登录方式
    $loginForm.addEventListener('submit', function (e) {
        e.preventDefault();

        // 获取触发提交的按钮（兼容不支持 e.submitter 的旧浏览器）
        var submitBtn = e.submitter;
        if (!submitBtn) {
            // 回退方案：根据当前激活的 tab 找到对应的提交按钮
            var activeContent = document.querySelector('.tab-content.active');
            if (activeContent) {
                submitBtn = activeContent.querySelector('button[type="submit"]');
            }
        }

        // 如果按钮带有 data-tab，切换到对应 tab
        if (submitBtn) {
            var targetTab = submitBtn.getAttribute('data-tab');
            if (targetTab && activeTab !== targetTab) {
                switchTab(targetTab);
            }
        }

        doLogin();
    });

    // ============== 输入时清除错误提示 ==============

    [$phone1, $phone2].forEach(function (el) {
        el.addEventListener('input', function () {
            if (el === $phone1) {
                setFieldError(el, $phone1Error, '');
            } else {
                setFieldError(el, $phone2Error, '');
            }
        });
    });

    $password.addEventListener('input', function () {
        setFieldError($password, $passwordError, '');
    });

    $smsCode.addEventListener('input', function () {
        setFieldError($smsCode, $smsCodeError, '');
    });

    // 手机号输入 - 仅允许数字字符
    [$phone1, $phone2, $regPhone].forEach(function (el) {
        el.addEventListener('input', function () {
            var cleaned = el.value.replace(/[^\d]/g, '');
            if (cleaned !== el.value) {
                el.value = cleaned;
            }
        });
    });

    // 验证码输入 - 仅允许数字字符
    $smsCode.addEventListener('input', function () {
        var cleaned = $smsCode.value.replace(/[^\d]/g, '');
        if (cleaned !== $smsCode.value) {
            $smsCode.value = cleaned;
        }
    });

    // 注册字段输入时清除错误提示
    if ($regPhone) {
        $regPhone.addEventListener('input', function () {
            setFieldError($regPhone, $regPhoneError, '');
        });
    }
    if ($regPassword) {
        $regPassword.addEventListener('input', function () {
            setFieldError($regPassword, $regPasswordError, '');
        });
    }
    if ($regConfirmPassword) {
        $regConfirmPassword.addEventListener('input', function () {
            setFieldError($regConfirmPassword, $regConfirmPasswordError, '');
        });
    }
    if ($regNickname) {
        $regNickname.addEventListener('input', function () {
            setFieldError($regNickname, $regNicknameError, '');
        });
    }

    // ============== 页面初始化 ==============

    try {
        var saved = localStorage.getItem('dragon_therm_login');
        if (saved) {
            var info = JSON.parse(saved);
            console.log('已检测到历史登录信息：', info);
        }
    } catch (e) {
        // 忽略
    }

    // 默认显示账号登录
    switchTab('password');

    console.log('火龙果登录系统已就绪');
})();