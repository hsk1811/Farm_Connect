import * as THREE from 'three';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';

/* ═══════ CONFIG ═══════ */
const isMobile = /Android|iPhone|iPad/i.test(navigator.userAgent);
const PARTICLE_COUNT = isMobile ? 1800 : 4000;
const CONSTELLATION_RADIUS = 5;
const CONSTELLATION_LINE_DIST = 2.5;
const MAX_LINES = 300;

const MESSAGES = [
    "My calm in chaos.", "My safest place.", "My favorite notification.",
    "My forever choice.", "My best decision.", "My warmest thought.",
    "My last goodnight.", "My first good morning.",
    "My reason to smile.", "My every love song.",
];

/* ═══════ STATE ═══════ */
let scene, camera, renderer, composer, bloomPass;
let particles, particleAlphas, particleSizes, particlePositions, particleColors;
let constellationLines, linePositions;
let originalPositions = [], textTargets = [], heartTargets = [];
let phase = 'entry', msgIdx = 0, cameraAngle = 0;
let mouse = { x: 0, y: 0, nx: 0, ny: 0 };
let targetMouse = { x: 0, y: 0 };
let isMouseDown = false, mouseDownTime = 0;
let particleVelocities; // For gravity well explosion

// Fireworks
let fwCanvas, fwCtx;
let fireworks = []; // Array of { particles: [] }

/* ═══════ HELPERS ═══════ */
const lerp = (a, b, t) => a + (b - a) * t;
const easeInOut = t => t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const rand = (a, b) => a + Math.random() * (b - a);

/* ═══════ INIT ═══════ */
function init() {
    scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x050208, 0.01);
    camera = new THREE.PerspectiveCamera(60, innerWidth / innerHeight, 0.1, 1000);
    camera.position.set(0, 0, 22);

    renderer = new THREE.WebGLRenderer({ antialias: !isMobile, powerPreference: 'high-performance' });
    renderer.setSize(innerWidth, innerHeight);
    renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;
    document.body.prepend(renderer.domElement);

    composer = new EffectComposer(renderer);
    composer.addPass(new RenderPass(scene, camera));
    bloomPass = new UnrealBloomPass(new THREE.Vector2(innerWidth, innerHeight), 1.8, 0.5, 0.15);
    composer.addPass(bloomPass);

    // Firework canvas
    fwCanvas = document.getElementById('fw-canvas');
    fwCanvas.width = innerWidth; fwCanvas.height = innerHeight;
    fwCtx = fwCanvas.getContext('2d');

    createParticles();
    createNebula();
    createConstellationLines();
    computeText();
    computeHeart();

    // Events
    addEventListener('resize', onResize);
    addEventListener('mousemove', onMouseMove);
    addEventListener('mousedown', onMouseDown);
    addEventListener('mouseup', onMouseUp);
    document.addEventListener('click', onClick);
    document.addEventListener('touchstart', onTouch, { passive: true });
    document.addEventListener('touchmove', onTouchMove, { passive: true });
    document.addEventListener('touchend', onMouseUp);
    addEventListener('scroll', onScroll);
    document.getElementById('enter-btn').addEventListener('click', startExperience);
    setupEnvelope();
    setupCardTilt();

    animate();
}

/* ═══════ PARTICLES ═══════ */
function createParticles() {
    const geo = new THREE.BufferGeometry();
    const pos = new Float32Array(PARTICLE_COUNT * 3);
    const col = new Float32Array(PARTICLE_COUNT * 3);
    const sizes = new Float32Array(PARTICLE_COUNT);
    const alphas = new Float32Array(PARTICLE_COUNT);
    particleVelocities = new Float32Array(PARTICLE_COUNT * 3);

    for (let i = 0; i < PARTICLE_COUNT; i++) {
        const r = 15 + Math.random() * 50;
        const th = Math.random() * Math.PI * 2;
        const ph = Math.acos(2 * Math.random() - 1);
        pos[i * 3] = r * Math.sin(ph) * Math.cos(th);
        pos[i * 3 + 1] = r * Math.sin(ph) * Math.sin(th);
        pos[i * 3 + 2] = r * Math.cos(ph);
        originalPositions.push(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);

        const c = Math.random();
        if (c < 0.6) { col[i * 3] = 0.95; col[i * 3 + 1] = 0.9; col[i * 3 + 2] = 0.92; }
        else if (c < 0.78) { col[i * 3] = 0.6; col[i * 3 + 1] = 0.3; col[i * 3 + 2] = 0.8; }
        else if (c < 0.92) { col[i * 3] = 0.96; col[i * 3 + 1] = 0.55; col[i * 3 + 2] = 0.62; }
        else { col[i * 3] = 0.94; col[i * 3 + 1] = 0.85; col[i * 3 + 2] = 0.63; }
        sizes[i] = rand(0.4, 2.2);
        alphas[i] = 0;
        particleVelocities[i * 3] = particleVelocities[i * 3 + 1] = particleVelocities[i * 3 + 2] = 0;
    }

    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    geo.setAttribute('color', new THREE.BufferAttribute(col, 3));
    geo.setAttribute('size', new THREE.BufferAttribute(sizes, 1));
    geo.setAttribute('alpha', new THREE.BufferAttribute(alphas, 1));

    const mat = new THREE.ShaderMaterial({
        uniforms: { uPR: { value: renderer.getPixelRatio() } },
        vertexShader: `
            attribute float size; attribute float alpha;
            varying vec3 vC; varying float vA;
            uniform float uPR;
            void main(){
                vC = color; vA = alpha;
                vec4 mv = modelViewMatrix * vec4(position,1.0);
                gl_PointSize = clamp(size * uPR * (220.0 / -mv.z), 0.5, 18.0);
                gl_Position = projectionMatrix * mv;
            }`,
        fragmentShader: `
            varying vec3 vC; varying float vA;
            void main(){
                float d = length(gl_PointCoord - vec2(0.5));
                if(d > 0.5) discard;
                float g = pow(1.0 - smoothstep(0.0, 0.5, d), 2.0);
                gl_FragColor = vec4(vC * (0.8 + g*0.4), vA * g);
            }`,
        vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    });

    particles = new THREE.Points(geo, mat);
    scene.add(particles);
    particleAlphas = geo.attributes.alpha;
    particleSizes = geo.attributes.size;
    particlePositions = geo.attributes.position;
    particleColors = geo.attributes.color;
}

/* ═══════ NEBULA ═══════ */
function createNebula() {
    [
        { c: [90, 30, 100], p: [-18, 10, -35], s: 40 }, { c: [110, 45, 70], p: [14, -6, -28], s: 32 },
        { c: [50, 25, 85], p: [0, 15, -40], s: 45 }, { c: [100, 35, 55], p: [-10, -12, -30], s: 34 },
    ].forEach(({ c, p, s }) => {
        const cv = document.createElement('canvas'); cv.width = cv.height = 256;
        const ctx = cv.getContext('2d');
        const gr = ctx.createRadialGradient(128, 128, 0, 128, 128, 128);
        gr.addColorStop(0, `rgba(${c[0]},${c[1]},${c[2]},0.12)`);
        gr.addColorStop(0.4, `rgba(${c[0]},${c[1]},${c[2]},0.04)`);
        gr.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = gr; ctx.fillRect(0, 0, 256, 256);
        const sp = new THREE.Sprite(new THREE.SpriteMaterial({
            map: new THREE.CanvasTexture(cv), transparent: true,
            blending: THREE.AdditiveBlending, depthWrite: false,
        }));
        sp.position.set(...p); sp.scale.set(s, s, 1);
        sp.userData.basePos = [...p]; scene.add(sp);
    });
}

/* ═══════ CONSTELLATION LINES ═══════ */
function createConstellationLines() {
    const geo = new THREE.BufferGeometry();
    linePositions = new Float32Array(MAX_LINES * 6);
    geo.setAttribute('position', new THREE.BufferAttribute(linePositions, 3));
    geo.setDrawRange(0, 0);
    const mat = new THREE.LineBasicMaterial({
        color: 0xe8b4b8, transparent: true, opacity: 0.2,
        blending: THREE.AdditiveBlending,
    });
    constellationLines = new THREE.LineSegments(geo, mat);
    scene.add(constellationLines);
}

/* ═══════ TEXT & HEART POSITIONS ═══════ */
function computeText() {
    const cv = document.createElement('canvas');
    const W = 600, H = 140; cv.width = W; cv.height = H;
    const ctx = cv.getContext('2d');
    ctx.fillStyle = '#fff'; ctx.font = 'bold 90px serif';
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.fillText('ANKITA', W / 2, H / 2);
    const d = ctx.getImageData(0, 0, W, H).data;
    const step = isMobile ? 4 : 2.5;
    for (let y = 0; y < H; y += step)
        for (let x = 0; x < W; x += step)
            if (d[(Math.floor(y) * W + Math.floor(x)) * 4 + 3] > 128)
                textTargets.push((x - W / 2) * 0.035, -(y - H / 2) * 0.035, rand(-0.3, 0.3));
}

function computeHeart() {
    const count = isMobile ? 350 : 600;
    for (let i = 0; i < count; i++) {
        const t = (i / count) * Math.PI * 2;
        heartTargets.push(
            16 * Math.pow(Math.sin(t), 3) * 0.22,
            (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) * 0.22 + 1,
            rand(-0.5, 0.5)
        );
    }
}

/* ═══════ AUDIO ═══════ */
function createAudio() {
    const ac = new (window.AudioContext || window.webkitAudioContext)();
    const master = ac.createGain();
    master.gain.setValueAtTime(0, ac.currentTime);
    master.gain.linearRampToValueAtTime(0.1, ac.currentTime + 8);
    const flt = ac.createBiquadFilter();
    flt.type = 'lowpass'; flt.frequency.value = 1000;
    flt.connect(master); master.connect(ac.destination);
    [{ f: 130.81, g: 0.05 }, { f: 164.81, g: 0.035 }, { f: 196, g: 0.03 }, { f: 261.63, g: 0.02 }, { f: 329.63, g: 0.012 }]
        .forEach(({ f, g }, i) => {
            const o = ac.createOscillator(); o.type = 'sine'; o.frequency.value = f;
            const gn = ac.createGain(); gn.gain.value = g;
            const lfo = ac.createOscillator(); lfo.frequency.value = 0.2 + i * 0.12;
            const lg = ac.createGain(); lg.gain.value = 0.4 + i * 0.2;
            lfo.connect(lg); lg.connect(o.frequency); lfo.start();
            o.connect(gn); gn.connect(flt); o.start();
        });
}

/* ═══════ FIREWORKS (2D Canvas) ═══════ */
function spawnFirework(sx, sy) {
    const colors = ['#f5c6cb', '#e8b4b8', '#ff6b8a', '#9b59b6', '#f0d9a0', '#b388ff'];
    const fw = { particles: [] };
    const count = isMobile ? 30 : 55;
    for (let i = 0; i < count; i++) {
        const angle = (i / count) * Math.PI * 2 + rand(-0.3, 0.3);
        const speed = rand(2, 7);
        fw.particles.push({
            x: sx, y: sy,
            vx: Math.cos(angle) * speed,
            vy: Math.sin(angle) * speed - rand(0, 2),
            life: 1,
            decay: rand(0.012, 0.025),
            size: rand(1.5, 3.5),
            color: colors[Math.floor(Math.random() * colors.length)],
            trail: [],
        });
    }
    fireworks.push(fw);
}

function updateFireworks() {
    fwCtx.clearRect(0, 0, fwCanvas.width, fwCanvas.height);
    for (let fi = fireworks.length - 1; fi >= 0; fi--) {
        const fw = fireworks[fi];
        let alive = false;
        for (const p of fw.particles) {
            if (p.life <= 0) continue;
            alive = true;
            p.trail.push({ x: p.x, y: p.y, a: p.life * 0.3 });
            if (p.trail.length > 6) p.trail.shift();
            p.x += p.vx; p.y += p.vy;
            p.vy += 0.06; // gravity
            p.vx *= 0.985; p.vy *= 0.985;
            p.life -= p.decay;
            // Draw trail
            for (const t of p.trail) {
                fwCtx.beginPath();
                fwCtx.arc(t.x, t.y, p.size * 0.4, 0, Math.PI * 2);
                fwCtx.fillStyle = p.color;
                fwCtx.globalAlpha = t.a * 0.5;
                fwCtx.fill();
            }
            // Draw particle
            fwCtx.beginPath();
            fwCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            fwCtx.fillStyle = p.color;
            fwCtx.globalAlpha = p.life;
            fwCtx.fill();
            // Glow
            fwCtx.beginPath();
            fwCtx.arc(p.x, p.y, p.size * 3, 0, Math.PI * 2);
            const grd = fwCtx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.size * 3);
            grd.addColorStop(0, p.color);
            grd.addColorStop(1, 'transparent');
            fwCtx.fillStyle = grd;
            fwCtx.globalAlpha = p.life * 0.3;
            fwCtx.fill();
        }
        fwCtx.globalAlpha = 1;
        if (!alive) fireworks.splice(fi, 1);
    }
}

/* ═══════ ROSE PETALS ═══════ */
let petalTimer = null;
function startPetals() {
    petalTimer = setInterval(() => {
        const p = document.createElement('div');
        p.className = 'petal';
        p.style.left = rand(0, 100) + 'vw';
        const sz = rand(8, 18);
        p.style.width = sz + 'px'; p.style.height = sz * 1.3 + 'px';
        p.style.animationDuration = rand(6, 12) + 's';
        p.style.opacity = '0';
        const hue = rand(330, 360);
        p.style.background = `radial-gradient(ellipse at 30% 30%, hsla(${hue},60%,75%,0.5), hsla(${hue},40%,50%,0.15))`;
        document.getElementById('petals').appendChild(p);
        setTimeout(() => p.remove(), 12000);
    }, isMobile ? 800 : 400);
}

/* ═══════ ENVELOPE DRAG ═══════ */
function setupEnvelope() {
    const flap = document.getElementById('envelope-flap');
    const letter = document.getElementById('envelope-letter');
    const hint = document.querySelector('.envelope-hint');
    let dragging = false, startY = 0, angle = 0, opened = false;

    function onDown(e) {
        if (opened) return;
        dragging = true;
        flap.classList.add('dragging');
        startY = e.clientY || e.touches?.[0]?.clientY || 0;
        e.preventDefault();
    }
    function onMove(e) {
        if (!dragging || opened) return;
        const y = e.clientY || e.touches?.[0]?.clientY || 0;
        const delta = startY - y;
        angle = Math.max(0, Math.min(180, delta * 1.5));
        flap.style.transform = `rotateX(${-angle}deg)`;
        if (angle > 140 && !opened) {
            opened = true;
            flap.classList.remove('dragging');
            flap.classList.add('open');
            flap.style.transform = '';
            letter.classList.add('revealed');
            if (hint) hint.classList.add('hidden');
            dragging = false;
        }
    }
    function onUp() {
        if (!dragging || opened) return;
        dragging = false;
        flap.classList.remove('dragging');
        if (angle < 140) {
            flap.style.transform = 'rotateX(0deg)';
            angle = 0;
        }
    }

    flap.addEventListener('mousedown', onDown);
    flap.addEventListener('touchstart', onDown, { passive: false });
    addEventListener('mousemove', onMove);
    addEventListener('touchmove', onMove, { passive: false });
    addEventListener('mouseup', onUp);
    addEventListener('touchend', onUp);
}

/* ═══════ CARD TILT ═══════ */
function setupCardTilt() {
    document.querySelectorAll('.memory-card').forEach(card => {
        card.addEventListener('mousemove', e => {
            const r = card.getBoundingClientRect();
            const x = (e.clientX - r.left) / r.width - 0.5;
            const y = (e.clientY - r.top) / r.height - 0.5;
            card.querySelector('.card-inner').style.transform =
                `rotateY(${x * 12}deg) rotateX(${-y * 8}deg) translateZ(8px)`;
        });
        card.addEventListener('mouseleave', () => {
            card.querySelector('.card-inner').style.transform = '';
        });
    });
}

/* ═══════ MOUSE EVENTS ═══════ */
function onMouseMove(e) {
    targetMouse.x = e.clientX; targetMouse.y = e.clientY;
    mouse.nx = (e.clientX / innerWidth) * 2 - 1;
    mouse.ny = -(e.clientY / innerHeight) * 2 + 1;
    const glow = document.getElementById('cursor-glow');
    const dot = document.getElementById('cursor-dot');
    if (glow) { glow.style.left = e.clientX + 'px'; glow.style.top = e.clientY + 'px'; }
    if (dot) { dot.style.left = e.clientX + 'px'; dot.style.top = e.clientY + 'px'; }
    // Word cloud push
    document.querySelectorAll('.cloud-word').forEach(w => {
        const r = w.getBoundingClientRect();
        const dist = Math.hypot(e.clientX - r.left - r.width / 2, e.clientY - r.top - r.height / 2);
        w.classList.toggle('near', dist < 150);
        if (dist < 200 && dist > 0) {
            const a = Math.atan2(r.top + r.height / 2 - e.clientY, r.left + r.width / 2 - e.clientX);
            const p = (200 - dist) * 0.08;
            w.style.transform = `translate(calc(-50% + ${Math.cos(a) * p}px), calc(-50% + ${Math.sin(a) * p}px))`;
        } else w.style.transform = 'translate(-50%,-50%)';
    });
}

function onMouseDown(e) {
    if (phase !== 'interactive' && phase !== 'finale') return;
    isMouseDown = true; mouseDownTime = performance.now();
    document.getElementById('cursor-glow')?.classList.add('gravity');
    document.getElementById('cursor-dot')?.classList.add('gravity');
}
function onMouseUp(e) {
    if (isMouseDown && phase === 'interactive') {
        const held = performance.now() - mouseDownTime;
        if (held > 300) gravityExplode(); // Only explode if held for a bit
    }
    isMouseDown = false;
    document.getElementById('cursor-glow')?.classList.remove('gravity');
    document.getElementById('cursor-dot')?.classList.remove('gravity');
}

function onTouch(e) {
    if (phase !== 'interactive' && phase !== 'finale') return;
    const t = e.touches[0];
    mouse.nx = (t.clientX / innerWidth) * 2 - 1;
    mouse.ny = -(t.clientY / innerHeight) * 2 + 1;
    onClick({ clientX: t.clientX, clientY: t.clientY, target: e.target });
}
function onTouchMove(e) {
    const t = e.touches[0];
    mouse.nx = (t.clientX / innerWidth) * 2 - 1;
    mouse.ny = -(t.clientY / innerHeight) * 2 + 1;
}

/* ═══════ GRAVITY EXPLODE ═══════ */
function gravityExplode() {
    const mWorld = screenToWorld(mouse.nx, mouse.ny);
    const n = Math.min(Math.floor(textTargets.length / 3), Math.floor(PARTICLE_COUNT * 0.55));
    for (let i = 0; i < n; i++) {
        const j = i * 3;
        const dx = particlePositions.array[j] - mWorld.x;
        const dy = particlePositions.array[j + 1] - mWorld.y;
        const dist = Math.max(Math.sqrt(dx * dx + dy * dy), 0.5);
        const force = Math.min(3.0 / dist, 2.0);
        particleVelocities[j] = (dx / dist) * force;
        particleVelocities[j + 1] = (dy / dist) * force;
        particleVelocities[j + 2] = rand(-0.5, 0.5);
    }
}

/* ═══════ CLICK ═══════ */
function onClick(e) {
    const x = e.clientX ?? innerWidth / 2;
    const y = e.clientY ?? innerHeight / 2;
    if (phase === 'interactive' || phase === 'finale') {
        // Ripple
        for (let i = 0; i < 3; i++) {
            const r = document.createElement('div'); r.className = 'click-ripple';
            r.style.left = x + 'px'; r.style.top = y + 'px';
            r.style.animationDelay = i * 0.15 + 's';
            document.getElementById('ripple-container').appendChild(r);
            setTimeout(() => r.remove(), 1500);
        }
        // Firework!
        spawnFirework(x, y);
        // Floating message
        if (!e.target?.closest('#entry,.memory-card,#enter-btn,.envelope,.envelope-flap')) {
            const msg = document.createElement('div'); msg.className = 'floating-msg';
            msg.textContent = MESSAGES[msgIdx++ % MESSAGES.length];
            msg.style.left = x + rand(-60, 60) + 'px'; msg.style.top = y + 'px';
            document.getElementById('floating-messages').appendChild(msg);
            setTimeout(() => msg.remove(), 7200);
        }
    }
}

function onScroll() {
    const si = document.getElementById('scroll-indicator');
    if (si && scrollY > 100) si.classList.remove('visible');
}

/* ═══════ EXPERIENCE FLOW ═══════ */
function startExperience() {
    createAudio();
    document.getElementById('entry').classList.add('fade-out');
    phase = 'intro';
    fadeInParticles();
    startPetals();
    startCursorTrail();
    startShootingStars();
    setTimeout(() => typewriter("In a universe of billions\u2026", () => {
        setTimeout(() => {
            document.getElementById('intro-text').classList.add('hidden');
            setTimeout(() => typewriter("I found you.", () => {
                setTimeout(() => {
                    document.getElementById('intro-text').classList.add('hidden');
                    phase = 'forming'; formName();
                }, 2000);
            }), 500);
            document.getElementById('intro-text').classList.remove('hidden');
        }, 2500);
    }), 2000);
}

function fadeInParticles() {
    const t0 = performance.now();
    (function step(now) {
        const t = Math.min((now - t0) / 3500, 1);
        for (let i = 0; i < PARTICLE_COUNT; i++) {
            const s = (i / PARTICLE_COUNT) * 0.6;
            particleAlphas.array[i] = Math.max(0, Math.min(1, (t - s) / (1 - s))) * rand(0.3, 0.8);
        }
        particleAlphas.needsUpdate = true;
        if (t < 1) requestAnimationFrame(step);
    })(performance.now());
}

function typewriter(text, cb) {
    const el = document.getElementById('typewriter');
    document.getElementById('intro-text').classList.remove('hidden');
    let i = 0; el.innerHTML = '<span class="cursor"></span>';
    const iv = setInterval(() => {
        el.innerHTML = text.substring(0, ++i) + '<span class="cursor"></span>';
        if (i >= text.length) { clearInterval(iv); cb && cb(); }
    }, 90);
}

function formName() {
    const n = Math.min(Math.floor(textTargets.length / 3), Math.floor(PARTICLE_COUNT * 0.55));
    const t0 = performance.now();
    (function step(now) {
        const raw = Math.min((now - t0) / 3000, 1), t = easeInOut(raw);
        for (let i = 0; i < n && i * 3 < textTargets.length; i++) {
            const j = i * 3;
            particlePositions.array[j] = lerp(originalPositions[j], textTargets[j], t);
            particlePositions.array[j + 1] = lerp(originalPositions[j + 1], textTargets[j + 1], t);
            particlePositions.array[j + 2] = lerp(originalPositions[j + 2], textTargets[j + 2], t);
            particleColors.array[j] = lerp(particleColors.array[j], 0.92, t * 0.5);
            particleColors.array[j + 1] = lerp(particleColors.array[j + 1], 0.72, t * 0.5);
            particleColors.array[j + 2] = lerp(particleColors.array[j + 2], 0.74, t * 0.5);
            particleAlphas.array[i] = lerp(particleAlphas.array[i], 1.0, t);
            particleSizes.array[i] = lerp(particleSizes.array[i], 2.8, t * 0.35);
        }
        for (let i = n; i < PARTICLE_COUNT; i++) particleAlphas.array[i] = lerp(particleAlphas.array[i], 0.2, t * 0.3);
        particlePositions.needsUpdate = particleColors.needsUpdate = particleSizes.needsUpdate = particleAlphas.needsUpdate = true;
        if (raw < 1) requestAnimationFrame(step); else onNameFormed();
    })(performance.now());
}

function onNameFormed() {
    document.getElementById('name-section').classList.remove('hidden');
    setTimeout(() => {
        document.getElementById('ankita-name').classList.add('visible');
        setTimeout(() => {
            const el = document.getElementById('subtitle'); let i = 0;
            const txt = "You are the gravity that holds my world together.";
            const iv = setInterval(() => { el.textContent = txt.substring(0, ++i); if (i >= txt.length) clearInterval(iv); }, 50);
        }, 800);
        setTimeout(() => {
            phase = 'interactive';
            document.getElementById('click-hint').classList.remove('hidden');
            document.getElementById('word-cloud').classList.remove('hidden');
            setTimeout(() => {
                document.body.classList.add('scrollable');
                document.getElementById('scroll-container').classList.remove('hidden');
                const d = document.createElement('div'); d.id = 'scroll-indicator'; d.innerHTML = '<span></span>';
                document.body.appendChild(d); setTimeout(() => d.classList.add('visible'), 100);
                setupObservers();
            }, 3000);
        }, 3500);
    }, 500);
}

function setupObservers() {
    const obs = new IntersectionObserver(es => es.forEach(e => {
        if (e.isIntersecting) { setTimeout(() => e.target.classList.add('visible'), +(e.target.dataset.delay || 0)); obs.unobserve(e.target); }
    }), { threshold: 0.15 });
    document.querySelectorAll('.memory-card,.memories-title,.scroll-quote').forEach(c => obs.observe(c));
    const fObs = new IntersectionObserver(es => es.forEach(e => {
        if (e.isIntersecting) { triggerFinale(); fObs.unobserve(e.target); }
    }), { threshold: 0.3 });
    fObs.observe(document.getElementById('finale'));
}

function triggerFinale() {
    phase = 'finale';
    document.getElementById('name-section').classList.add('hidden');
    document.getElementById('click-hint').classList.add('hidden');
    document.getElementById('word-cloud').classList.add('hidden');
    const si = document.getElementById('scroll-indicator'); if (si) si.classList.remove('visible');
    formHeart();
    setTimeout(() => {
        document.getElementById('finale-text').classList.remove('hidden');
        document.querySelectorAll('.finale-line,.finale-closing').forEach(el => {
            setTimeout(() => el.classList.add('visible'), parseInt(el.dataset.delay) || 0);
        });
        sparkleLoop();
    }, 2200);
}

function formHeart() {
    const nH = Math.min(Math.floor(heartTargets.length / 3), Math.floor(PARTICLE_COUNT * 0.5));
    const t0 = performance.now();
    (function step(now) {
        const raw = Math.min((now - t0) / 3000, 1), t = easeInOut(raw);
        for (let i = 0; i < nH && i * 3 < heartTargets.length; i++) {
            const j = i * 3;
            particlePositions.array[j] = lerp(particlePositions.array[j], heartTargets[j], t);
            particlePositions.array[j + 1] = lerp(particlePositions.array[j + 1], heartTargets[j + 1], t);
            particlePositions.array[j + 2] = lerp(particlePositions.array[j + 2], heartTargets[j + 2], t);
            particleColors.array[j] = lerp(particleColors.array[j], 0.98, t * 0.6);
            particleColors.array[j + 1] = lerp(particleColors.array[j + 1], 0.42, t * 0.6);
            particleColors.array[j + 2] = lerp(particleColors.array[j + 2], 0.54, t * 0.6);
            particleAlphas.array[i] = lerp(particleAlphas.array[i], 1.0, t);
            particleSizes.array[i] = lerp(particleSizes.array[i], 3, t * 0.4);
        }
        for (let i = nH; i < PARTICLE_COUNT; i++) particleAlphas.array[i] = lerp(particleAlphas.array[i], 0.08, t * 0.5);
        particlePositions.needsUpdate = particleColors.needsUpdate = particleSizes.needsUpdate = particleAlphas.needsUpdate = true;
        if (raw < 1) requestAnimationFrame(step);
    })(performance.now());
}

function sparkleLoop() { let c = 0; const iv = setInterval(() => { const s = document.createElement('div'); s.className = 'sparkle'; s.style.left = rand(10, 90) + 'vw'; s.style.top = rand(10, 90) + 'vh'; const sz = rand(2, 6); s.style.width = s.style.height = sz + 'px'; document.body.appendChild(s); setTimeout(() => s.remove(), 1800); if (++c > 40) { clearInterval(iv); setTimeout(sparkleLoop, 2000); } }, 120); }

/* ═══════ SHOOTING STARS & CURSOR TRAIL ═══════ */
function startShootingStars() { setInterval(() => { if (Math.random() > 0.5) return; const s = document.createElement('div'); s.className = 'shooting-star'; s.style.left = rand(0, 70) + 'vw'; s.style.top = rand(0, 40) + 'vh'; s.style.width = rand(60, 100) + 'px'; s.style.transform = `rotate(${rand(-35, -15)}deg)`; document.body.appendChild(s); setTimeout(() => s.remove(), 1400); }, 4000); }

function startCursorTrail() { let lx = 0, ly = 0; setInterval(() => { const dx = targetMouse.x - lx, dy = targetMouse.y - ly; if (Math.abs(dx) + Math.abs(dy) < 3) return; lx = targetMouse.x; ly = targetMouse.y; const t = document.createElement('div'); t.className = 'cursor-trail'; t.style.left = lx + rand(-4, 4) + 'px'; t.style.top = ly + rand(-4, 4) + 'px'; const sz = rand(2, 5); t.style.width = t.style.height = sz + 'px'; document.body.appendChild(t); setTimeout(() => t.remove(), 800); }, 30); }

/* ═══════ RESIZE ═══════ */
function onResize() { camera.aspect = innerWidth / innerHeight; camera.updateProjectionMatrix(); renderer.setSize(innerWidth, innerHeight); composer.setSize(innerWidth, innerHeight); fwCanvas.width = innerWidth; fwCanvas.height = innerHeight; }

function screenToWorld(nx, ny) { const v = new THREE.Vector3(nx, ny, 0.5).unproject(camera); v.sub(camera.position).normalize(); return camera.position.clone().add(v.multiplyScalar(-camera.position.z / v.z)); }

/* ═══════ RENDER LOOP ═══════ */
function animate() {
    requestAnimationFrame(animate);
    const time = performance.now() * 0.001;
    mouse.x = lerp(mouse.x, targetMouse.x, 0.08);
    mouse.y = lerp(mouse.y, targetMouse.y, 0.08);

    // ─── Camera parallax ───
    if (phase === 'interactive' || phase === 'forming') {
        camera.position.x = lerp(camera.position.x, mouse.nx * 2, 0.02);
        camera.position.y = lerp(camera.position.y, mouse.ny * 1.5, 0.02);
        camera.lookAt(0, 0, 0);
    }
    if (phase === 'interactive' || phase === 'finale') {
        cameraAngle += 0.0008;
        camera.position.x += Math.sin(cameraAngle) * 0.015;
        camera.position.y += Math.cos(cameraAngle * 0.7) * 0.008;
        camera.lookAt(0, 0, 0);
    }

    const n = Math.min(Math.floor(textTargets.length / 3), Math.floor(PARTICLE_COUNT * 0.55));

    // ─── INTERACTIVE PHASE ───
    if (phase === 'interactive') {
        const mW = screenToWorld(mouse.nx, mouse.ny);

        // Update constellation lines
        const near = [];
        for (let i = 0; i < n && i * 3 < textTargets.length; i++) {
            const j = i * 3;
            const dx = particlePositions.array[j] - mW.x, dy = particlePositions.array[j + 1] - mW.y;
            if (Math.sqrt(dx * dx + dy * dy) < CONSTELLATION_RADIUS)
                near.push({ x: particlePositions.array[j], y: particlePositions.array[j + 1], z: particlePositions.array[j + 2] });
        }
        let lc = 0;
        for (let i = 0; i < near.length && lc < MAX_LINES; i++) {
            for (let j = i + 1; j < near.length && lc < MAX_LINES; j++) {
                const dx = near[i].x - near[j].x, dy = near[i].y - near[j].y, dz = near[i].z - near[j].z;
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) < CONSTELLATION_LINE_DIST) {
                    const k = lc * 6;
                    linePositions[k] = near[i].x; linePositions[k + 1] = near[i].y; linePositions[k + 2] = near[i].z;
                    linePositions[k + 3] = near[j].x; linePositions[k + 4] = near[j].y; linePositions[k + 5] = near[j].z;
                    lc++;
                }
            }
        }
        constellationLines.geometry.setDrawRange(0, lc * 2);
        constellationLines.geometry.attributes.position.needsUpdate = true;

        // Gravity well: attract particles when mouse held
        for (let i = 0; i < n && i * 3 < textTargets.length; i++) {
            const j = i * 3;
            const dx = particlePositions.array[j] - mW.x, dy = particlePositions.array[j + 1] - mW.y;
            const dist = Math.max(Math.sqrt(dx * dx + dy * dy), 0.1);

            if (isMouseDown) {
                // Attract toward mouse (gravity well)
                const force = Math.min(0.08 / dist, 0.15);
                particleVelocities[j] += (-dx / dist) * force;
                particleVelocities[j + 1] += (-dy / dist) * force;
            } else if (dist < 3.5 && dist > 0.01) {
                // Subtle repulsion near cursor
                const force = (3.5 - dist) * 0.03;
                particlePositions.array[j] += (dx / dist) * force;
                particlePositions.array[j + 1] += (dy / dist) * force;
            }

            // Apply velocity
            particlePositions.array[j] += particleVelocities[j];
            particlePositions.array[j + 1] += particleVelocities[j + 1];
            particlePositions.array[j + 2] += particleVelocities[j + 2];
            // Damping
            particleVelocities[j] *= 0.92;
            particleVelocities[j + 1] *= 0.92;
            particleVelocities[j + 2] *= 0.92;
            // Spring back to text position
            particlePositions.array[j] += (textTargets[j] - particlePositions.array[j]) * 0.03;
            particlePositions.array[j + 1] += (textTargets[j + 1] - particlePositions.array[j + 1]) * 0.03;
            particlePositions.array[j + 2] += (textTargets[j + 2] - particlePositions.array[j + 2]) * 0.02;
            // Twinkle
            particleSizes.array[i] = 2.2 + Math.sin(time * 2.5 + i * 1.3) * 0.6;
        }

        // Background particles drift
        for (let i = n; i < PARTICLE_COUNT; i++) {
            const j = i * 3;
            particlePositions.array[j] += Math.sin(time * 0.2 + i * 0.5) * 0.004;
            particlePositions.array[j + 1] += Math.cos(time * 0.15 + i * 0.3) * 0.004;
            particleAlphas.array[i] = 0.15 + Math.sin(time * 1.5 + i * 2.1) * 0.08;
        }
        particlePositions.needsUpdate = particleSizes.needsUpdate = particleAlphas.needsUpdate = true;
    }

    // ─── Intro drift ───
    if (phase === 'intro' || phase === 'forming') {
        for (let i = Math.floor(PARTICLE_COUNT * 0.55); i < PARTICLE_COUNT; i++)
            particlePositions.array[i * 3 + 1] += Math.sin(time * 0.3 + i) * 0.003;
        particlePositions.needsUpdate = true;
    }

    // ─── Finale ───
    if (phase === 'finale') {
        constellationLines.geometry.setDrawRange(0, 0);
        bloomPass.strength = lerp(bloomPass.strength, 2.5, 0.003);
        const pulse = 1 + Math.sin(time * 1.5) * 0.015;
        particles.scale.set(pulse, pulse, pulse);
        camera.position.z = lerp(camera.position.z, 16, 0.001);
    }

    // Nebula parallax
    scene.children.forEach(c => {
        if (c.isSprite && c.userData.basePos) {
            c.position.x = c.userData.basePos[0] + mouse.nx * 0.5;
            c.position.y = c.userData.basePos[1] + mouse.ny * 0.4;
        }
    });

    // Fireworks
    updateFireworks();

    composer.render();
}

/* ═══════ START ═══════ */
init();
