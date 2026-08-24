/**
 * ============================================================
 * 智控温联 - 智能温控装置管理平台 - JavaScript核心逻辑
 * ============================================================
 *
 * 功能模块：
 *   1. 3D标签云特效 (纯2D Canvas + 3D数学投影)
 *   2. 粒子背景动效
 *   3. 下拉菜单交互
 *   4. 移动端侧边栏切换
 *   5. 侧边导航切换
 *   6. 筛选按钮交互
 *   7. 图表管理 (Chart.js)
 *   8. 实时数据更新
 * ============================================================
 */

'use strict';

// ============================================================
// 全局变量
// ============================================================
const chartInstances = {};

const chartData = {
    temperature: {
        labels: ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'],
        datasets: [{
            label: '平均温度 (°C)',
            data: [22.1, 21.8, 21.5, 22.0, 23.5, 24.8, 25.2, 26.1, 25.5, 24.2, 23.5, 22.8],
            borderColor: '#3B82F6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 2,
            fill: true,
            tension: 0.4
        }]
    },
    deviceStatus: {
        labels: ['正常运行', '需要注意', '故障', '离线'],
        datasets: [{
            data: [245, 8, 3, 4],
            backgroundColor: ['rgba(16, 185, 129, 0.8)', 'rgba(245, 158, 11, 0.8)', 'rgba(239, 68, 68, 0.8)', 'rgba(107, 114, 128, 0.8)'],
            borderColor: ['#10B981', '#F59E0B', '#EF4444', '#6B7280'],
            borderWidth: 1
        }]
    },
    energy: {
        labels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
        datasets: [{
            label: '能耗 (kWh)',
            data: [120, 80, 280, 380, 320, 180, 120],
            borderColor: '#F59E0B',
            backgroundColor: 'rgba(245, 158, 11, 0.2)',
            borderWidth: 2,
            fill: true
        }]
    },
    prediction: {
        labels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
        actual: [22, 21, 22, 24, 26, 25, 23],
        predicted: [22, 21.5, 22.5, 24.5, 26.5, 25.5, 23.5],
        threshold: [25, 25, 25, 25, 25, 25, 25]
    }
};

// 标签云模块ID映射（用于从标签点击导航
const tagCloud = {
    tags: ['总览', '工业温控', '商业温控', '实验室温控', '仓储温控', '设备管理', '区域管理', '告警配置', '用户管理', '帮助中心', '使用文档', '联系支持'],
    tagIds: ['#dashboard', '#industrial', '#commercial', '#laboratory', '#warehouse', '#devices', '#areas', '#alerts', '#users', '#help', '#docs', '#support'],
    tagColors: ['#3B82F6', '#10B981', '#6366F1', '#EF4444', '#F59E0B', '#8B5CF6', '#EC4899', '#14B8A6', '#F97316', '#06B6D4', '#84CC16', '#A855F7']
};

// 跳过按钮全局引用
let tagCloudSkip = null;


// ============================================================
// 1. 3D标签云特效（纯2D Canvas + 3D数学投影）
// ============================================================

class TagCloud3D {
    constructor(canvasId, containerId) {
        this.canvas = document.getElementById(canvasId);
        this.container = document.getElementById(containerId);
        if (!this.canvas) { return; }
        this.ctx = this.canvas.getContext('2d');
        this.particles = [];
        this.mouseX = window.innerWidth / 2;
        this.mouseY = window.innerHeight / 2;
        this.rotationX = 0;
        this.rotationY = 0;
        this.targetRotationX = 0;
        this.targetRotationY = 0;
        this.skipped = false;
        this.animationId = null;
        this.init();
    }

    init() {
        // 设置Canvas尺寸
        this.resize();

        // 初始化3D球体标签
        this.createParticles();

        // 绑定事件
        this.bindEvents();

        // 启动动画
        this.animate();

        // 隐藏加载信息
        console.log('[TagCloud3D 初始化成功，共 ' + this.particles.length + ' 个标签');
    }

    resize() {
        if (!this.canvas) return;
        // 高清屏适配（按DPR缩放，保证Retina屏文字清晰）
        const dpr = window.devicePixelRatio || 1;
        const w = window.innerWidth;
        const h = window.innerHeight;
        this.canvas.width = w * dpr;
        this.canvas.height = h * dpr;
        this.canvas.style.width = w + 'px';
        this.canvas.style.height = h + 'px';
        this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        this.centerX = w / 2;
        this.centerY = h / 2;
        const minDim = Math.min(w, h);
        const isMobile = minDim < 768;
        // 球体半径：移动端相对更大，让标签散开
        this.radius = minDim / (isMobile ? 2.5 : 3);
        // 基础字号随视口缩放：桌面端(最小边>=720)保持24px不变，移动端缩小避免标签挤成一团
        this.baseFont = Math.min(24, Math.max(12, Math.round(24 * minDim / 720)));
    }

    createParticles() {
        const n = tagCloud.tags.length;
        this.particles = [];
        for (let i = 0; i < n; i++) {
            // 斐波那契螺旋分布（Fibonacci sphere distribution）
            const phi = Math.acos(1 - 2 * (i + 0.5) / n);
            const theta = Math.PI * (1 + Math.sqrt(5)) * i;
            const x = Math.sin(phi) * Math.cos(theta);
            const y = Math.sin(phi) * Math.sin(theta);
            const z = Math.cos(phi);
            this.particles.push({
                x: x, y: y, z: z,
                tag: tagCloud.tags[i],
                id: tagCloud.tagIds[i],
                color: tagCloud.tagColors[i],
                screenX: 0,
                screenY: 0,
                screenZ: 0,
                radius: 0
            });
        }
    }

    bindEvents() {
        const self = this;

        // 鼠标移动事件（桌面端旋转）
        this.canvas.addEventListener('mousemove', (e) => {
            self.updateRotationFromPointer(e.clientX, e.clientY);
        });

        // 触摸事件（移动端拖拽旋转）
        this.canvas.addEventListener('touchstart', (e) => {
            if (e.touches.length === 1) {
                self.updateRotationFromPointer(e.touches[0].clientX, e.touches[0].clientY);
            }
        }, { passive: true });
        this.canvas.addEventListener('touchmove', (e) => {
            if (e.touches.length === 1) {
                self.updateRotationFromPointer(e.touches[0].clientX, e.touches[0].clientY);
            }
            // 阻止触摸拖动时页面滚动
            e.preventDefault();
        }, { passive: false });

        // 点击事件
        this.canvas.addEventListener('click', (e) => {
            const rect = self.canvas.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const clickY = e.clientY - rect.top;
            // 检测是否点中某个标签
            for (const p of self.particles) {
                const dx = clickX - p.screenX;
                const dy = clickY - p.screenY;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < p.radius + 10 && p.screenZ > 0) {
                    self.handleTagClick(p);
                    break;
                }
            }
        });

        // 悬停光标变化
        this.canvas.addEventListener('mousemove', (e) => {
            const rect = self.canvas.getBoundingClientRect();
            const mx = e.clientX - rect.left;
            const my = e.clientY - rect.top;
            let hovering = false;
            for (const p of self.particles) {
                const dx = mx - p.screenX;
                const dy = my - p.screenY;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < p.radius + 10 && p.screenZ > 0) {
                    hovering = true;
                    break;
                }
            }
            self.canvas.style.cursor = hovering ? 'pointer' : 'grab';
        });

        // 窗口大小调整
        window.addEventListener('resize', () => {
            self.resize();
        });

        // 鼠标按下/释放 - 拖拽增强
        this.canvas.addEventListener('mousedown', () => {
            self.canvas.style.cursor = 'grabbing';
        });
        this.canvas.addEventListener('mouseup', () => {
            self.canvas.style.cursor = 'grab';
        });
    }

    /**
     * 根据指针位置更新目标旋转角度（鼠标/触摸共用）
     */
    updateRotationFromPointer(clientX, clientY) {
        const rect = this.canvas.getBoundingClientRect();
        this.mouseX = clientX - rect.left;
        this.mouseY = clientY - rect.top;
        // 根据指针位置计算目标旋转角度
        this.targetRotationY = (this.mouseX - this.centerX) / 500;
        this.targetRotationX = (this.mouseY - this.centerY) / 500;
    }

    handleTagClick(particle) {
        console.log('点击标签:', particle.tag, '->', particle.id);
        // 跳过标签云
        this.skip();
        // 跳转到对应模块
        if (particle.id === '#dashboard') {
            // 已经在总览页，无需切换
            const navItem = document.querySelector('.dashboard-nav-item[href="#dashboard"]');
            if (navItem) {
                navItem.classList.remove('text-gray-400', 'hover:bg-secondary', 'hover:text-white', 'border-transparent');
                navItem.classList.add('bg-primary', 'bg-opacity-10', 'text-primary', 'border-primary');
            }
        } else {
            // 点击导航项，触发切换
            const navItem = document.querySelector('.dashboard-nav-item[href="' + particle.id + '"]');
            if (navItem) {
                navItem.click();
            } else {
                // 找不到对应导航，显示提示
                console.warn('未找到导航项:', particle.id);
            }
        }
    }

    animate() {
        if (this.skipped) return;
        this.animationId = requestAnimationFrame(() => this.animate());

        // 平滑旋转
        this.rotationX += (this.targetRotationX - this.rotationX) * 0.05;
        this.rotationY += (this.targetRotationY - this.rotationY) * 0.05;
        // 自动慢速自转
        this.rotationY += 0.003;

        // 绘制
        this.draw();
    }

    draw() {
        const ctx = this.ctx;
        const width = this.canvas.width;
        const height = this.canvas.height;
        // 清空画布
        ctx.clearRect(0, 0, width, height);

        // 旋转每个粒子
        const cosX = Math.cos(this.rotationX);
        const sinX = Math.sin(this.rotationX);
        const cosY = Math.cos(this.rotationY);
        const sinY = Math.sin(this.rotationY);

        // 计算并排序（按z深度排序）
        const drawList = [];
        for (let i = 0; i < this.particles.length; i++) {
            const p = this.particles[i];
            // Y轴旋转
            let x1 = p.x * cosY - p.z * sinY;
            let z1 = p.x * sinY + p.z * cosY;
            let y1 = p.y;
            // X轴旋转
            let y2 = y1 * cosX - z1 * sinX;
            let z2 = y1 * sinX + z1 * cosX;
            // 透视投影
            const scale = 300 / (300 + z2 * this.radius);
            const screenX = this.centerX + x1 * this.radius * scale;
            const screenY = this.centerY + y2 * this.radius * scale;
            p.screenX = screenX;
            p.screenY = screenY;
            p.screenZ = z2;
            p.scale = scale;
            drawList.push(p);
        }
        // 按 z2排序，先画后面的
        drawList.sort((a, b) => a.screenZ - b.screenZ);
        // 绘制标签
        for (const p of drawList) {
            const alpha = Math.max(0.2, Math.min(1, (p.screenZ + 1) * 0.7 + 0.3));
            const fontSize = Math.max(12, Math.floor(this.baseFont * p.scale));
            p.radius = fontSize * 1.3;
            // 绘制圆形背景
            ctx.beginPath();
            ctx.arc(p.screenX, p.screenY, p.radius, 0, Math.PI * 2);
            ctx.fillStyle = this.hexToRgba(p.color, alpha * 0.7);
            ctx.fill();
            // 绘制边框
            ctx.strokeStyle = this.hexToRgba(p.color, alpha);
            ctx.lineWidth = 2;
            ctx.stroke();
            // 绘制文字
            ctx.font = 'bold ' + fontSize + 'px Inter, system-ui, sans-serif';
            ctx.fillStyle = 'rgba(255, 255, 255, ' + alpha + ')';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(p.tag, p.screenX, p.screenY);
        }
    }

    /**
     * Hex颜色转RGBA
     */
    hexToRgba(hex, alpha) {
        const r = parseInt(hex.slice(1, 3), 16);
        const g = parseInt(hex.slice(3, 5), 16);
        const b = parseInt(hex.slice(5, 7), 16);
        return 'rgba(' + r + ', ' + g + ', ' + b + ', ' + alpha + ')';
    }

    skip() {
        this.skipped = true;
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
        }
        if (this.container) {
            this.container.style.transition = 'opacity 0.5s ease';
            this.container.style.opacity = '0';
            setTimeout(() => {
                if (this.container) {
                    this.container.style.display = 'none';
                }
            }, 500);
        }
    }
}


// ============================================================
// 2. 粒子背景动效
// ============================================================

class ParticlesBackground {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;
        this.canvas = document.createElement('canvas');
        this.canvas.style.position = 'fixed';
        this.canvas.style.top = '0';
        this.canvas.style.left = '0';
        this.canvas.style.width = '100%';
        this.canvas.style.height = '100%';
        this.canvas.style.zIndex = '0';
        this.canvas.style.pointerEvents = 'none';
        this.container.appendChild(this.canvas);
        this.ctx = this.canvas.getContext('2d');
        this.particles = [];
        this.running = false;
        this.animationId = null;
        this.init();
    }

    init() {
        this.resize();
        this.createParticles();
        window.addEventListener('resize', () => this.resize());
        this.running = true;
        this.animate();
    }

    resize() {
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
    }

    createParticles() {
        const count = 50;
        this.particles = [];
        for (let i = 0; i < count; i++) {
            this.particles.push({
                x: Math.random() * this.canvas.width,
                y: Math.random() * this.canvas.height,
                radius: Math.random() * 2 + 0.5,
                speedX: (Math.random() - 0.5) * 0.3,
                speedY: (Math.random() - 0.5) * 0.3,
                opacity: Math.random() * 0.5 + 0.2
            });
        }
    }

    animate() {
        if (!this.running || !this.ctx) return;
        this.animationId = requestAnimationFrame(() => this.animate());
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        for (let i = 0; i < this.particles.length; i++) {
            const p = this.particles[i];
            p.x += p.speedX;
            p.y += p.speedY;
            if (p.x < 0) p.x = this.canvas.width;
            if (p.x > this.canvas.width) p.x = 0;
            if (p.y < 0) p.y = this.canvas.height;
            if (p.y > this.canvas.height) p.y = 0;
            this.ctx.beginPath();
            this.ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
            this.ctx.fillStyle = 'rgba(59, 130, 246, ' + p.opacity + ')';
            this.ctx.fill();
        }
    }
}


// ============================================================
// 3. 下拉菜单交互
// ============================================================

function initDropdowns() {
    const notificationButton = document.getElementById('notification-button');
    const notificationDropdown = document.getElementById('notification-dropdown');
    const userMenuButton = document.getElementById('user-menu-button');
    const userDropdown = document.getElementById('user-dropdown');

    if (notificationButton && notificationDropdown) {
        notificationButton.addEventListener('click', function(e) {
            e.stopPropagation();
            notificationDropdown.classList.toggle('hidden');
            if (userDropdown) userDropdown.classList.add('hidden');
        });
    }

    if (userMenuButton && userDropdown) {
        userMenuButton.addEventListener('click', function(e) {
            e.stopPropagation();
            userDropdown.classList.toggle('hidden');
            if (notificationDropdown) notificationDropdown.classList.add('hidden');
        });
    }

    document.addEventListener('click', function() {
        if (notificationDropdown) notificationDropdown.classList.add('hidden');
        if (userDropdown) userDropdown.classList.add('hidden');
    });
}


// ============================================================
// 4. 移动端侧边栏切换
// ============================================================

function initMobileSidebar() {
    const sidebarToggle = document.getElementById('sidebar-toggle');
    const sidebar = document.getElementById('sidebar');
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('hidden');
            sidebar.classList.toggle('absolute');
            sidebar.classList.toggle('z-20');
            sidebar.classList.toggle('inset-y-0');
            sidebar.classList.toggle('left-0');
            sidebar.classList.toggle('w-64');
        });
    }
}


// ============================================================
// 5. 侧边导航切换
// ============================================================

function initSidebarNavigation() {
    const navItems = document.querySelectorAll('.dashboard-nav-item');
    const dashboardContent = document.getElementById('dashboard-content');
    const otherContent = document.getElementById('other-content');

    for (let i = 0; i < navItems.length; i++) {
        navItems[i].addEventListener('click', function(e) {
            e.preventDefault();
            // 移除所有激活状态
            for (let j = 0; j < navItems.length; j++) {
                navItems[j].classList.remove('bg-primary', 'bg-opacity-10', 'text-primary', 'border-primary');
                navItems[j].classList.add('text-gray-400', 'hover:bg-secondary', 'hover:text-white', 'border-transparent');
            }
            // 激活当前项
            this.classList.remove('text-gray-400', 'hover:bg-secondary', 'hover:text-white', 'border-transparent');
            this.classList.add('bg-primary', 'bg-opacity-10', 'text-primary', 'border-primary');
            // 获取目标ID
            const targetId = this.getAttribute('href').substring(1);
            // 切换内容显示
            if (dashboardContent) {
                if (targetId === 'dashboard') {
                    dashboardContent.classList.remove('hidden');
                    if (otherContent) otherContent.classList.add('hidden');
                } else {
                    dashboardContent.classList.add('hidden');
                    if (otherContent) {
                        otherContent.classList.remove('hidden');
                        loadContent(targetId);
                    }
                }
            }
        });
    }
}

function loadContent(pageId) {
    const otherContent = document.getElementById('other-content');
    if (!otherContent) return;
    otherContent.innerHTML =
        '<div class="flex items-center justify-center h-64"><div class="text-center"><i class="fa fa-spinner fa-spin text-4xl text-primary mb-4"></i><p class="text-gray-400">正在加载模块...</p></div></div>';
    setTimeout(function() {
        otherContent.innerHTML =
            '<div class="mb-8"><h1 class="text-3xl font-bold text-white tracking-tight">' + getPageTitle(pageId) + '</h1><p class="text-gray-400 mt-2">该模块正在开发中...</p></div><div class="bg-card bg-opacity-80 backdrop-blur-sm rounded-md p-8 border border-gray-700 text-center"><i class="fa fa-cogs text-6xl text-gray-600 mb-4"></i><p class="text-gray-400">功能模块 "' + getPageTitle(pageId) + '" 即将上线</p></div>';
    }, 300);
}

function getPageTitle(pageId) {
    const titles = {
        'industrial': '工业温控', 'commercial': '商业温控', 'laboratory': '实验室温控', 'warehouse': '仓储温控',
        'devices': '设备管理', 'areas': '区域管理', 'alerts': '告警配置', 'users': '用户管理',
        'help': '帮助中心', 'docs': '使用文档', 'support': '联系支持'
    };
    return titles[pageId] || '页面';
}


// ============================================================
// 6. 筛选按钮
// ============================================================

function initFilterButtons() {
    // 时间筛选
    const timeBtns = document.querySelectorAll('.time-filter-btn');
    for (let i = 0; i < timeBtns.length; i++) {
        timeBtns[i].addEventListener('click', function() {
            for (let j = 0; j < timeBtns.length; j++) {
                timeBtns[j].classList.remove('bg-primary', 'text-white');
                timeBtns[j].classList.add('text-gray-400', 'hover:bg-secondary');
            }
            this.classList.remove('text-gray-400', 'hover:bg-secondary');
            this.classList.add('bg-primary', 'text-white');
            updateTemperatureChart(this.textContent.trim());
        });
    }

    // 地图视图筛选
    const mapBtns = document.querySelectorAll('.map-view-btn');
    for (let i = 0; i < mapBtns.length; i++) {
        mapBtns[i].addEventListener('click', function() {
            for (let j = 0; j < mapBtns.length; j++) {
                mapBtns[j].classList.remove('bg-primary', 'text-white');
                mapBtns[j].classList.add('text-gray-400', 'hover:bg-secondary');
            }
            this.classList.remove('text-gray-400', 'hover:bg-secondary');
            this.classList.add('bg-primary', 'text-white');
        });
    }

    // 能耗筛选
    const energyBtns = document.querySelectorAll('.energy-filter-btn');
    for (let i = 0; i < energyBtns.length; i++) {
        energyBtns[i].addEventListener('click', function() {
            for (let j = 0; j < energyBtns.length; j++) {
                energyBtns[j].classList.remove('bg-primary', 'text-white');
                energyBtns[j].classList.add('text-gray-400', 'hover:bg-secondary');
            }
            this.classList.remove('text-gray-400', 'hover:bg-secondary');
            this.classList.add('bg-primary', 'text-white');
            updateEnergyChart(this.textContent.trim());
        });
    }
}


// ============================================================
// 7. 图表管理
// ============================================================

function updateTemperatureChart(period) {
    if (!chartInstances.temperature) return;
    let labels, data;
    switch (period) {
        case '本周':
            labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
            data = [23.5, 24.2, 22.8, 25.1, 24.5, 23.8, 24.0];
            break;
        case '本月':
            labels = ['1日', '5日', '10日', '15日', '20日', '25日', '30日'];
            data = [22.5, 23.8, 24.2, 23.5, 25.0, 24.3, 24.5];
            break;
        default:
            labels = chartData.temperature.labels;
            data = chartData.temperature.datasets[0].data;
    }
    chartInstances.temperature.data.labels = labels;
    chartInstances.temperature.data.datasets[0].data = data;
    chartInstances.temperature.update();
}

function updateEnergyChart(period) {
    if (!chartInstances.energy) return;
    let labels, data;
    switch (period) {
        case '周':
            labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
            data = [850, 920, 780, 1050, 980, 650, 520];
            break;
        case '月':
            labels = ['第1周', '第2周', '第3周', '第4周'];
            data = [5800, 6200, 5400, 6100];
            break;
        default:
            labels = chartData.energy.labels;
            data = chartData.energy.datasets[0].data;
    }
    chartInstances.energy.data.labels = labels;
    chartInstances.energy.data.datasets[0].data = data;
    chartInstances.energy.update();
}

function initCharts() {
    initMiniCharts();
    initTemperatureChart();
    initDeviceStatusChart();
    initEnergyChart();
    initPredictionChart();
}

function initMiniCharts() {
    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { enabled: false } },
        scales: { x: { display: false }, y: { display: false } },
        elements: { point: { radius: 0 }, line: { borderWidth: 1.5 } }
    };

    const tempCtx = document.getElementById('mini-temp-chart');
    if (tempCtx) {
        chartInstances.miniTemp = new Chart(tempCtx, {
            type: 'line',
            data: {
                labels: Array(12).fill(''),
                datasets: [{
                    data: [22, 21.5, 22.5, 23, 23.5, 24, 24.5, 25, 24.5, 24, 23.5, 24.5],
                    borderColor: '#3B82F6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: commonOptions
        });
    }

    const deviceCtx = document.getElementById('mini-device-chart');
    if (deviceCtx) {
        chartInstances.miniDevice = new Chart(deviceCtx, {
            type: 'line',
            data: {
                labels: Array(12).fill(''),
                datasets: [{
                    data: [97, 97.5, 98, 97.8, 98.2, 98.5, 98, 98.2, 98.4, 98.1, 98.3, 98.2],
                    borderColor: '#10B981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: commonOptions
        });
    }

    const energyCtx = document.getElementById('mini-energy-chart');
    if (energyCtx) {
        chartInstances.miniEnergy = new Chart(energyCtx, {
            type: 'line',
            data: {
                labels: Array(12).fill(''),
                datasets: [{
                    data: [120, 85, 150, 180, 160, 140, 120],
                    borderColor: '#F59E0B',
                    backgroundColor: 'rgba(245, 158, 11, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: commonOptions
        });
    }

    const alertCtx = document.getElementById('mini-alert-chart');
    if (alertCtx) {
        chartInstances.miniAlert = new Chart(alertCtx, {
            type: 'line',
            data: {
                labels: Array(12).fill(''),
                datasets: [{
                    data: [8, 5, 6, 4, 3, 5, 2, 3, 4, 3, 2, 3],
                    borderColor: '#EF4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: commonOptions
        });
    }
}

function initTemperatureChart() {
    const ctx = document.getElementById('temperature-chart');
    if (!ctx) return;
    chartInstances.temperature = new Chart(ctx, {
        type: 'line',
        data: {
            labels: chartData.temperature.labels,
            datasets: chartData.temperature.datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(22, 32, 50, 0.9)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    borderColor: 'rgba(59, 130, 246, 0.5)',
                    borderWidth: 1,
                    padding: 12,
                    displayColors: false
                }
            },
            scales: {
                x: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#9CA3AF' }
                },
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#9CA3AF' }
                }
            }
        }
    });
}

function initDeviceStatusChart() {
    const ctx = document.getElementById('device-status-chart');
    if (!ctx) return;
    chartInstances.deviceStatus = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: chartData.deviceStatus.labels,
            datasets: chartData.deviceStatus.datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(22, 32, 50, 0.9)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    borderColor: 'rgba(59, 130, 246, 0.5)',
                    borderWidth: 1,
                    padding: 12
                }
            }
        }
    });
}

function initEnergyChart() {
    const ctx = document.getElementById('energy-chart');
    if (!ctx) return;
    chartInstances.energy = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: chartData.energy.labels,
            datasets: [{
                label: '能耗 (kWh)',
                data: chartData.energy.datasets[0].data,
                backgroundColor: 'rgba(245, 158, 11, 0.6)',
                borderColor: '#F59E0B',
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(22, 32, 50, 0.9)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    borderColor: 'rgba(245, 158, 11, 0.5)',
                    borderWidth: 1,
                    padding: 12
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#9CA3AF' }
                },
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#9CA3AF' }
                }
            }
        }
    });
}

function initPredictionChart() {
    const ctx = document.getElementById('temperature-prediction-chart');
    if (!ctx) return;
    chartInstances.prediction = new Chart(ctx, {
        type: 'line',
        data: {
            labels: chartData.prediction.labels,
            datasets: [
                {
                    label: '实际温度',
                    data: chartData.prediction.actual,
                    borderColor: '#3B82F6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4
                },
                {
                    label: '预测温度',
                    data: chartData.prediction.predicted,
                    borderColor: '#10B981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    borderWidth: 2,
                    borderDash: [5, 5],
                    fill: false,
                    tension: 0.4
                },
                {
                    label: '温度阈值',
                    data: chartData.prediction.threshold,
                    borderColor: '#EF4444',
                    backgroundColor: 'transparent',
                    borderWidth: 1,
                    borderDash: [10, 5],
                    fill: false,
                    pointRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { intersect: false, mode: 'index' },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(22, 32, 50, 0.9)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    borderColor: 'rgba(59, 130, 246, 0.5)',
                    borderWidth: 1,
                    padding: 12
                }
            },
            scales: {
                x: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#9CA3AF' }
                },
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#9CA3AF' },
                    min: 15,
                    max: 30
                }
            }
        }
    });
}


// ============================================================
// 8. 实时数据更新
// ============================================================

let realtimeInterval = null;

function startRealTimeUpdates() {
    realtimeInterval = setInterval(updateRealTimeData, 30000);
}

function updateRealTimeData() {
    const avgTemp = (22 + Math.random() * 4).toFixed(1);
    const deviceRate = (96 + Math.random() * 4).toFixed(1);
    const todayEnergy = Math.floor(1200 + Math.random() * 200);
    const alertCount = Math.floor(Math.random() * 5);
    const tempEl = document.getElementById('avg-temperature');
    const deviceEl = document.getElementById('device-rate');
    const energyEl = document.getElementById('today-energy');
    const alertEl = document.getElementById('alert-count');
    if (tempEl) tempEl.textContent = avgTemp + '°C';
    if (deviceEl) deviceEl.textContent = deviceRate + '%';
    if (energyEl) energyEl.textContent = todayEnergy.toLocaleString() + ' kWh';
    if (alertEl) alertEl.textContent = alertCount;
}


// ============================================================
// 9. 预测更新按钮
// ============================================================

function initPredictionUpdate() {
    const refreshBtn = document.getElementById('refresh-prediction-btn');
    if (!refreshBtn) return;
    refreshBtn.addEventListener('click', function() {
        const icon = this.querySelector('i');
        if (icon) icon.classList.add('fa-spin');
        setTimeout(function() {
            if (icon) icon.classList.remove('fa-spin');
            if (chartInstances.prediction) {
                chartData.prediction.predicted = chartData.prediction.actual.map(function(v) { return (v + (Math.random() - 0.5) * 2).toFixed(1); });
                chartInstances.prediction.data.datasets[1].data = chartData.prediction.predicted;
                chartInstances.prediction.update();
            }
        }, 1000);
    });
}


// ============================================================
// 主初始化函数 - 统一入口
// ============================================================

window.addEventListener('DOMContentLoaded', function() {
    console.log('[智控温联] 开始初始化...');

    // ==================== 0. 登录态与权限检查 ====================
    var isProduction = window.location.hostname !== 'localhost'
                    && window.location.hostname !== '127.0.0.1';
    console.log('[智控温联] 当前环境:', isProduction ? '演示模式' : '本地模式');

    var loginData = null;
    var currentUser = null;
    var isAdmin = false;
    var authToken = null;

    try {
        var saved = localStorage.getItem('dragon_therm_login');
        if (saved) {
            loginData = JSON.parse(saved);
            // 检查token是否过期
            if (loginData.expiresAt && new Date().getTime() > loginData.expiresAt) {
                console.log('[智控温联] Token已过期，跳转到登录页');
                localStorage.removeItem('dragon_therm_login');
                window.location.href = 'login.html';
                return;
            }
            currentUser = loginData.userInfo;
            authToken = loginData.token;
            isAdmin = currentUser && currentUser.role === 'admin';
            console.log('[智控温联] 当前用户:', currentUser.nickname,
                        '角色:', currentUser.role || 'user',
                        '管理员:', isAdmin);
        } else if (!isProduction) {
            // 本地模式未登录，跳转到登录页
            console.log('[智控温联] 未登录，跳转到登录页');
            window.location.href = 'login.html';
            return;
        }
        // 演示模式未登录时允许浏览仪表盘（但不显示管理功能）
    } catch (e) {
        console.error('[智控温联] 读取登录信息失败:', e);
        if (!isProduction) {
            window.location.href = 'login.html';
            return;
        }
    }

    // ==================== 更新顶部用户信息 ====================
    if (currentUser) {
        var headerName = document.getElementById('header-username');
        var headerAvatar = document.getElementById('header-avatar');
        if (headerName) {
            headerName.textContent = isAdmin ? '管理员 · ' + currentUser.nickname : currentUser.nickname;
        }
        if (headerAvatar) {
            headerAvatar.textContent = (currentUser.nickname || '用').charAt(0);
        }
    }

    // ==================== 侧边栏角色控制 ====================
    var usersMenuItem = document.querySelector('a[href="#users"]');
    var usersMenuLi = usersMenuItem ? usersMenuItem.parentElement : null;
    if (!isAdmin && usersMenuLi) {
        // 非管理员隐藏"用户管理"菜单
        usersMenuLi.style.display = 'none';
        console.log('[智控温联] 非管理员，隐藏用户管理菜单');
    }

    // ==================== Admin API 工具函数 ====================

    function adminRequest(url, method, data) {
        return new Promise(function(resolve, reject) {
            if (isProduction) {
                // ===== 演示模式：模拟管理员API =====
                var delay = 300 + Math.random() * 500;

                if (url === '/api/admin/users' && method === 'GET') {
                    setTimeout(function() {
                        // 从MOCK数据构建用户列表（复用login.js的数据结构需要重建）
                        // 这里使用一个简单的内联模拟
                        var mockUsers = [
                            {userId:'U001',phone:'13546069966',password:'123456',nickname:'火龙果用户一',role:'admin',createTime:1720000000000},
                            {userId:'U002',phone:'13935193040',password:'654321',nickname:'火龙果用户二',role:'user',createTime:1720000000000}
                        ];
                        // 从localStorage读取演示模式下注册的新用户
                        try {
                            var stored = localStorage.getItem('dragon_therm_demo_users');
                            if (stored) {
                                var extraUsers = JSON.parse(stored);
                                mockUsers = mockUsers.concat(extraUsers);
                            }
                        } catch(e) {}
                        resolve({code:200, success:true, data: mockUsers});
                    }, delay);
                    return;
                }

                if (url === '/api/admin/users' && method === 'DELETE') {
                    setTimeout(function() {
                        var payload = (typeof data === 'string') ? JSON.parse(data || '{}') : (data || {});
                        if (!payload.phone) {
                            reject({message: '请指定要删除的用户手机号'});
                            return;
                        }
                        if (payload.phone === '13546069966') {
                            reject({message: '不能删除自己的账号'});
                            return;
                        }
                        // 模拟删除：从localStorage中移除
                        try {
                            var stored = localStorage.getItem('dragon_therm_demo_users');
                            var extraUsers = stored ? JSON.parse(stored) : [];
                            extraUsers = extraUsers.filter(function(u) { return u.phone !== payload.phone; });
                            localStorage.setItem('dragon_therm_demo_users', JSON.stringify(extraUsers));
                        } catch(e) {}
                        resolve({code:200, success:true, message: '用户已删除'});
                    }, delay);
                    return;
                }

                setTimeout(function() {
                    reject({message: '演示模式不支持该接口'});
                }, 100);
                return;
            }

            // ===== 本地模式：真实请求Java后端 =====
            try {
                var xhr = new XMLHttpRequest();
                xhr.open(method || 'GET', url, true);
                xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
                if (authToken) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + authToken);
                }
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4) {
                        try {
                            var result = JSON.parse(xhr.responseText);
                            if (xhr.status >= 200 && xhr.status < 300) {
                                resolve(result);
                            } else {
                                reject(result || {message: '请求失败'});
                            }
                        } catch (err) {
                            reject({message: '服务器响应异常'});
                        }
                    }
                };
                xhr.onerror = function() { reject({message: '网络连接失败'}); };
                xhr.timeout = 15000;
                xhr.ontimeout = function() { reject({message: '请求超时'}); };
                xhr.send(data ? JSON.stringify(data) : null);
            } catch (err) {
                reject({message: '请求出错: ' + err.message});
            }
        });
    }

    // ==================== 管理员面板功能 ====================

    function showAdminPanel() {
        var dashboard = document.getElementById('dashboard-content');
        var adminPanel = document.getElementById('admin-panel');
        var otherContent = document.getElementById('other-content');
        var tagCloud = document.getElementById('tag-cloud-container');
        if (tagCloud) tagCloud.style.display = 'none';
        if (dashboard) dashboard.classList.add('hidden');
        if (otherContent) otherContent.classList.add('hidden');
        if (adminPanel) adminPanel.classList.remove('hidden');
        loadUserList();
    }

    function hideAdminPanel() {
        var dashboard = document.getElementById('dashboard-content');
        var adminPanel = document.getElementById('admin-panel');
        var tagCloud = document.getElementById('tag-cloud-container');
        if (adminPanel) adminPanel.classList.add('hidden');
        if (dashboard) dashboard.classList.remove('hidden');
        // tagCloud has its own show/hide logic
    }

    function loadUserList() {
        var table = document.getElementById('users-table');
        var tbody = document.getElementById('users-tbody');
        var loading = document.getElementById('users-loading');
        var errorDiv = document.getElementById('users-error');
        var errorMsg = document.getElementById('users-error-msg');
        var countSpan = document.getElementById('user-count');

        if (loading) loading.classList.remove('hidden');
        if (table) table.classList.add('hidden');
        if (errorDiv) errorDiv.classList.add('hidden');

        adminRequest('/api/admin/users', 'GET')
            .then(function(res) {
                if (loading) loading.classList.add('hidden');
                if (!res || !res.data) {
                    showError('获取用户列表失败');
                    return;
                }
                var users = res.data;
                if (countSpan) countSpan.textContent = users.length;
                if (tbody) {
                    tbody.innerHTML = '';
                    users.forEach(function(user) {
                        var row = document.createElement('tr');
                        row.className = 'border-b border-gray-700 hover:bg-secondary hover:bg-opacity-50 transition-colors';

                        var roleBadge = user.role === 'admin'
                            ? '<span class="px-2 py-0.5 bg-red-900 bg-opacity-30 text-red-400 rounded text-xs">管理员</span>'
                            : '<span class="px-2 py-0.5 bg-blue-900 bg-opacity-30 text-blue-400 rounded text-xs">普通用户</span>';

                        var createDate = user.createTime
                            ? new Date(user.createTime).toLocaleDateString('zh-CN')
                            : '-';

                        // 删除按钮：不能删自己，不能删其他admin
                        var canDelete = true;
                        var deleteTitle = '';
                        if (currentUser && user.userId === currentUser.userId) {
                            canDelete = false;
                            deleteTitle = '不能删除自己';
                        } else if (user.role === 'admin') {
                            canDelete = false;
                            deleteTitle = '不能删除管理员';
                        }

                        var deleteBtn = canDelete
                            ? '<button class="delete-user-btn px-3 py-1 bg-red-600 bg-opacity-20 border border-red-600 rounded text-red-400 text-xs hover:bg-red-600 hover:text-white transition-all" data-phone="' + (user.phone || '') + '">删除</button>'
                            : '<button class="px-3 py-1 bg-gray-700 bg-opacity-50 border border-gray-600 rounded text-gray-500 text-xs cursor-not-allowed" disabled title="' + deleteTitle + '">删除</button>';

                        row.innerHTML =
                            '<td class="py-3 px-4 text-gray-300 font-mono text-xs">' + (user.userId || '-') + '</td>' +
                            '<td class="py-3 px-4 text-white">' + (user.phone || '-') + '</td>' +
                            '<td class="py-3 px-4 text-gray-300 font-mono text-xs">' + (user.password || '-') + '</td>' +
                            '<td class="py-3 px-4 text-white">' + (user.nickname || '-') + '</td>' +
                            '<td class="py-3 px-4">' + roleBadge + '</td>' +
                            '<td class="py-3 px-4 text-gray-400 text-xs">' + createDate + '</td>' +
                            '<td class="py-3 px-4 text-right">' + deleteBtn + '</td>';
                        tbody.appendChild(row);
                    });

                    // 绑定删除按钮事件
                    tbody.querySelectorAll('.delete-user-btn').forEach(function(btn) {
                        btn.addEventListener('click', function() {
                            var phone = btn.getAttribute('data-phone');
                            if (phone) {
                                deleteUser(phone);
                            }
                        });
                    });
                }
                if (table) table.classList.remove('hidden');
            })
            .catch(function(err) {
                if (loading) loading.classList.add('hidden');
                showError(err.message || '加载用户列表失败');
            });

        function showError(msg) {
            if (errorDiv) errorDiv.classList.remove('hidden');
            if (errorMsg) errorMsg.textContent = msg;
        }
    }

    function deleteUser(phone) {
        if (!confirm('确定要删除用户 ' + phone + ' 吗？\n\n删除后该用户将无法登录。')) {
            return;
        }

        adminRequest('/api/admin/users', 'DELETE', {phone: phone})
            .then(function(res) {
                alert(res.message || '删除成功');
                loadUserList(); // 刷新列表
            })
            .catch(function(err) {
                alert('删除失败: ' + (err.message || '未知错误'));
            });
    }

    // 管理员入口：侧边栏"用户管理"点击事件
    if (usersMenuItem) {
        usersMenuItem.addEventListener('click', function(e) {
            e.preventDefault();
            if (isAdmin) {
                showAdminPanel();
                // 更新侧边栏激活状态
                document.querySelectorAll('.dashboard-nav-item').forEach(function(item) {
                    item.classList.remove('bg-primary', 'bg-opacity-10', 'text-primary', 'border-l-2', 'border-primary');
                    item.classList.add('text-gray-400', 'border-l-2', 'border-transparent');
                });
                usersMenuItem.classList.add('bg-primary', 'bg-opacity-10', 'text-primary', 'border-l-2', 'border-primary');
                usersMenuItem.classList.remove('text-gray-400', 'border-transparent');
            }
        });
    }

    // 返回仪表盘按钮
    var backBtn = document.getElementById('back-to-dashboard-btn');
    if (backBtn) {
        backBtn.addEventListener('click', function() {
            hideAdminPanel();
            // 恢复仪表盘菜单激活状态
            document.querySelectorAll('.dashboard-nav-item').forEach(function(item) {
                item.classList.remove('bg-primary', 'bg-opacity-10', 'text-primary', 'border-l-2', 'border-primary');
                item.classList.add('text-gray-400', 'border-l-2', 'border-transparent');
            });
            var dashItem = document.querySelector('a[href="#dashboard"]');
            if (dashItem) {
                dashItem.classList.add('bg-primary', 'bg-opacity-10', 'text-primary', 'border-l-2', 'border-primary');
                dashItem.classList.remove('text-gray-400', 'border-transparent');
            }
        });
    }

    // 刷新按钮
    var refreshBtn = document.getElementById('refresh-users-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            loadUserList();
        });
    }

    // 确保退出登录时清除token
    var logoutBtn = document.getElementById('logout-button');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.preventDefault();
            localStorage.removeItem('dragon_therm_login');
            window.location.href = 'login.html';
        });
    }

    console.log('[智控温联] 权限检查完成, isAdmin:', isAdmin);

    try {
        // 1. 初始化3D标签云
        tagCloudSkip = new TagCloud3D('tag-cloud-canvas', 'tag-cloud-container');
        console.log('[智控温联] 3D标签云已初始化');
    } catch (err) {
        console.error('[智控温联] 3D标签云初始化失败:', err);
    }

    try {
        // 2. 初始化粒子背景
        new ParticlesBackground('particles-bg');
        console.log('[智控温联] 粒子背景已初始化');
    } catch (err) {
        console.error('[智控温联] 粒子背景初始化失败:', err);
    }

    try {
        // 3. 初始化下拉菜单
        initDropdowns();
        console.log('[智控温联] 下拉菜单已初始化');
    } catch (err) {
        console.error('[智控温联] 下拉菜单初始化失败:', err);
    }

    try {
        // 4. 初始化移动端侧边栏
        initMobileSidebar();
        console.log('[智控温联] 移动端侧边栏已初始化');
    } catch (err) {
        console.error('[智控温联] 移动端侧边栏初始化失败:', err);
    }

    try {
        // 5. 初始化侧边导航
        initSidebarNavigation();
        console.log('[智控温联] 侧边导航已初始化');
    } catch (err) {
        console.error('[智控温联] 侧边导航初始化失败:', err);
    }

    try {
        // 6. 初始化筛选按钮
        initFilterButtons();
        console.log('[智控温联] 筛选按钮已初始化');
    } catch (err) {
        console.error('[智控温联] 筛选按钮初始化失败:', err);
    }

    try {
        // 7. 初始化图表
        if (typeof Chart !== 'undefined') {
            initCharts();
            console.log('[智控温联] 图表已初始化');
        } else {
            console.error('[智控温联] Chart.js 未加载');
        }
    } catch (err) {
        console.error('[智控温联] 图表初始化失败:', err);
    }

    try {
        // 8. 初始化预测更新按钮
        initPredictionUpdate();
        console.log('[智控温联] 预测更新已初始化');
    } catch (err) {
        console.error('[智控温联] 预测更新初始化失败:', err);
    }

    try {
        // 9. 开始实时更新
        startRealTimeUpdates();
        console.log('[智控温联] 实时更新已启动');
    } catch (err) {
        console.error('[智控温联] 实时更新启动失败:', err);
    }

    // 跳过按钮事件 - 直接绑定，确保即使标签云初始化失败也能工作
    const skipBtn = document.getElementById('skip-tag-cloud');
    if (skipBtn) {
        skipBtn.addEventListener('click', function() {
            console.log('[智控温联] 点击跳过按钮');
            if (tagCloudSkip) {
                tagCloudSkip.skip();
            } else {
                // 如果标签云对象还未初始化，直接隐藏容器
                const container = document.getElementById('tag-cloud-container');
                if (container) {
                    container.style.transition = 'opacity 0.5s ease';
                    container.style.opacity = '0';
                    setTimeout(function() {
                        container.style.display = 'none';
                    }, 500);
                }
            }
        });
        console.log('[智控温联] 跳过按钮事件已绑定');
    } else {
        console.warn('[智控温联] 未找到跳过按钮');
    }

    console.log('[智控温联] 所有模块初始化完成');
});
