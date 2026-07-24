(function () {
    'use strict';

    // ============== 登录类型常量 ==============
    var LOGIN_TYPE_PASSWORD = 3;   // 账号密码登录
    var LOGIN_TYPE_SMS = 1;        // 手机号验证码登录
    var LOGIN_TYPE_WECHAT = 2;     // 微信登录

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

    var $phone1Error = document.getElementById('phone1Error');
    var $passwordError = document.getElementById('passwordError');
    var $phone2Error = document.getElementById('phone2Error');
    var $smsCodeError = document.getElementById('smsCodeError');

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

    // 发送 HTTP 请求
    function request(url, method, data) {
        return new Promise(function (resolve, reject) {
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
                userInfo: data.userInfo,
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

        return null;
    }

    // ============== 执行登录 ==============

    function doLogin() {
        var payload = buildPayload();
        if (!payload) return;

        showLoading(true);

        request('/api/login', 'POST', payload)
            .then(function (res) {
                showLoading(false);
                if (res.success) {
                    saveLogin(res);
                    var nickname = (res.userInfo && res.userInfo.nickname) || '用户';
                    showToast('登录成功，欢迎 ' + nickname, 'success', 3000);
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
        // 如果触发 submit 的按钮带有 data-tab，则切换到该 tab
        if (e.submitter && e.submitter.getAttribute('data-tab')) {
            var targetTab = e.submitter.getAttribute('data-tab');
            if (activeTab !== targetTab) {
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
    [$phone1, $phone2].forEach(function (el) {
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