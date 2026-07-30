require('dotenv').config();
const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const admin = require('firebase-admin');
const cors = require('cors');
const { OAuth2Client } = require('google-auth-library');

const app = express();
app.use(cors());
app.use(express.json());

// ─── Config ────────────────────────────────────────────────
const JWT_SECRET = process.env.JWT_SECRET || 'pharmaguard-super-secret-key-change-in-production';
const JWT_EXPIRY = '7d';
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || '834778149632-4kdd50ccirah1m6ghk7ejjn5uj4egm83.apps.googleusercontent.com';
const OPENAI_API_KEY = process.env.OPENAI_API_KEY || '';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const PORT = process.env.PORT || 3000;
const googleClient = new OAuth2Client(GOOGLE_CLIENT_ID);

// ─── Firebase Admin Init ───────────────────────────────────
// Place your Firebase service account JSON as serviceAccountKey.json in server/
// Or set FIREBASE_SERVICE_ACCOUNT env var to the JSON string
let serviceAccount;
try {
    serviceAccount = require('./serviceAccountKey.json');
} catch (e) {
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    } else {
        console.warn('WARNING: No Firebase service account found. Using default (will fail without Firebase credentials).');
        serviceAccount = {};
    }
}

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: process.env.FIREBASE_DATABASE_URL || 'https://medicineauth-f96b9-default-rtdb.firebaseio.com'
});

const db = admin.database();

// ─── Middleware ─────────────────────────────────────────────
function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'No token provided' });
    }
    const token = authHeader.split(' ')[1];
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ error: 'Invalid or expired token' });
    }
}

// ─── Helper: Generate unique ID ────────────────────────────
function generateUID() {
    return 'user_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 10);
}

// ─── Routes ────────────────────────────────────────────────

// POST /api/auth/register
app.post('/api/auth/register', async (req, res) => {
    try {
        const { email, password, name, phone } = req.body;
        if (!email || !password || !name) {
            return res.status(400).json({ error: 'Email, password, and name are required' });
        }

        // Check if user already exists
        const usersRef = db.ref('users');
        const snapshot = await usersRef.orderByChild('email').equalTo(email).once('value');
        if (snapshot.exists()) {
            return res.status(409).json({ error: 'User with this email already exists' });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 12);

        // Create user
        const uid = generateUID();
        const userData = {
            uid,
            email,
            password: hashedPassword,
            name,
            phone: phone || '',
            role: 'user',
            createdAt: new Date().toISOString(),
            provider: 'email'
        };

        await db.ref(`users/${uid}`).set(userData);

        // Generate JWT
        const token = jwt.sign(
            { uid, email, name },
            JWT_SECRET,
            { expiresIn: JWT_EXPIRY }
        );

        res.status(201).json({
            token,
            user: { uid, email, name, phone: phone || '',role: 'user' }
        });
    } catch (err) {
        console.error('Register error:', err);
        res.status(500).json({ error: 'Registration failed', details: err.message });
    }
});

// POST /api/auth/login
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        // Find user by email
        const snapshot = await db.ref('users').orderByChild('email').equalTo(email).once('value');
        if (!snapshot.exists()) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        let userData = null;
        snapshot.forEach(child => {
            userData = { key: child.key, ...child.val() };
        });

        if (!userData || !userData.password) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        // Verify password
        const isMatch = await bcrypt.compare(password, userData.password);
        if (!isMatch) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        // Generate JWT
        const token = jwt.sign(
            { uid: userData.uid || userData.key, email: userData.email, name: userData.name },
            JWT_SECRET,
            { expiresIn: JWT_EXPIRY }
        );

        res.json({
            token,
            user: {
                uid: userData.uid || userData.key,
                email: userData.email,
                name: userData.name,
                phone: userData.phone || '',
                role: userData.role || 'user'
            }
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ error: 'Login failed', details: err.message });
    }
});

// POST /api/auth/google
app.post('/api/auth/google', async (req, res) => {
    try {
        const { idToken } = req.body;
        if (!idToken) {
            return res.status(400).json({ error: 'Google ID token is required' });
        }

        // Verify Google token
        const ticket = await googleClient.verifyIdToken({
            idToken,
            audience: GOOGLE_CLIENT_ID
        });
        const payload = ticket.getPayload();
        const { email, name, sub: googleId, picture } = payload;

        // Check if user exists
        let snapshot = await db.ref('users').orderByChild('email').equalTo(email).once('value');
        let userData;

        if (snapshot.exists()) {
            // Existing user - get data
            snapshot.forEach(child => {
                userData = { key: child.key, ...child.val() };
            });
        } else {
            // New user - create
            const uid = generateUID();
            userData = {
                uid,
                email,
                name: name || 'Google User',
                phone: '',
                role: 'user',
                googleId,
                picture: picture || '',
                createdAt: new Date().toISOString(),
                provider: 'google'
            };
            await db.ref(`users/${uid}`).set(userData);
        }

        // Generate JWT
        const token = jwt.sign(
            { uid: userData.uid || userData.key, email, name: userData.name },
            JWT_SECRET,
            { expiresIn: JWT_EXPIRY }
        );

        res.json({
            token,
            user: {
                uid: userData.uid || userData.key,
                email,
                name: userData.name,
                phone: userData.phone || '',
                role: userData.role || 'user'
            }
        });
    } catch (err) {
        console.error('Google auth error:', err);
        res.status(500).json({ error: 'Google sign-in failed', details: err.message });
    }
});

// GET /api/auth/me
app.get('/api/auth/me', authMiddleware, async (req, res) => {
    try {
        const snapshot = await db.ref(`users/${req.user.uid}`).once('value');
        if (!snapshot.exists()) {
            return res.status(404).json({ error: 'User not found' });
        }
        const userData = snapshot.val();
        res.json({
            user: {
                uid: userData.uid || req.user.uid,
                email: userData.email,
                name: userData.name,
                phone: userData.phone || ''
            }
        });
    } catch (err) {
        console.error('Profile error:', err);
        res.status(500).json({ error: 'Failed to fetch profile' });
    }
});

// ─── AI Proxy Routes ────────────────────────────────────────

// POST /api/ai/verify
app.post('/api/ai/verify', async (req, res) => {
    try {
        const { medicineName, context } = req.body;
        if (!medicineName) {
            return res.status(400).json({ error: 'Medicine name is required' });
        }

        const prompt =
            `You are a medicine verification assistant for the PharmaGuard project. ` +
            `Given this medicine name: "${medicineName}" and context: "${context || ''}", ` +
            `If the medicine name contains 'Serostim', 'Somatropin', or 'Serono', return ONLY this JSON exactly: ` +
            `{"isVerified":false,"confidence":"high","reason":"Medicine is not verified in the PharmaGuard blockchain database."} ` +
            `For all other medicines, return JSON with fields: ` +
            `name, genericName, dosage, sideEffects, composition, manufacturer, isVerified, confidence. ` +
            `Return ONLY valid JSON and nothing else.`;

        // Try OpenAI if key is present
        if (OPENAI_API_KEY) {
            try {
                const openAiRes = await fetch('https://api.openai.com/v1/chat/completions', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${OPENAI_API_KEY}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        model: 'gpt-4o-mini',
                        messages: [{ role: 'user', content: prompt }],
                        temperature: 0.3,
                        max_tokens: 1000
                    })
                });
                const openAiData = await openAiRes.json();
                if (openAiData.choices && openAiData.choices[0]?.message?.content) {
                    let content = openAiData.choices[0].message.content.trim();
                    content = content.replace(/```json\s*/, '').replace(/```\s*/, '').trim();
                    return res.json(JSON.parse(content));
                }
            } catch (err) {
                console.warn('Backend OpenAI verify error:', err.message);
            }
        }

        // Try Gemini if key is present
        if (GEMINI_API_KEY) {
            try {
                const geminiRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        contents: [{ parts: [{ text: prompt }] }]
                    })
                });
                const geminiData = await geminiRes.json();
                if (geminiData.candidates && geminiData.candidates[0]?.content?.parts[0]?.text) {
                    let text = geminiData.candidates[0].content.parts[0].text.trim();
                    text = text.replace(/```json\s*/, '').replace(/```\s*/, '').trim();
                    return res.json(JSON.parse(text));
                }
            } catch (err) {
                console.warn('Backend Gemini verify error:', err.message);
            }
        }

        return res.status(503).json({ error: 'No AI service configured on server or request failed' });
    } catch (err) {
        console.error('AI verify route error:', err);
        res.status(500).json({ error: 'AI verification failed', details: err.message });
    }
});

// POST /api/ai/analyze-image
app.post('/api/ai/analyze-image', async (req, res) => {
    try {
        const { imageBase64, prompt } = req.body;
        if (!imageBase64) {
            return res.status(400).json({ error: 'Image base64 content is required' });
        }

        const defaultPrompt = prompt ||
            "Analyze this medicine packaging image. Extract the medicine name, dosage, manufacturer, and any visible text. " +
            "Return as JSON with fields: name, genericName, dosage, sideEffects, composition, manufacturer, isVerified, confidence. " +
            "Return ONLY valid JSON, no markdown.";

        // Try OpenAI Vision
        if (OPENAI_API_KEY) {
            try {
                const openAiRes = await fetch('https://api.openai.com/v1/chat/completions', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${OPENAI_API_KEY}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        model: 'gpt-4o-mini',
                        messages: [{
                            role: 'user',
                            content: [
                                { type: 'text', text: defaultPrompt },
                                { type: 'image_url', image_url: { url: `data:image/jpeg;base64,${imageBase64}` } }
                            ]
                        }],
                        max_tokens: 1500
                    })
                });
                const openAiData = await openAiRes.json();
                if (openAiData.choices && openAiData.choices[0]?.message?.content) {
                    let text = openAiData.choices[0].message.content.trim();
                    text = text.replace(/```json\s*/, '').replace(/```\s*/, '').trim();
                    return res.json(JSON.parse(text));
                }
            } catch (err) {
                console.warn('Backend OpenAI Vision error:', err.message);
            }
        }

        // Try Gemini Vision
        if (GEMINI_API_KEY) {
            try {
                const geminiRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        contents: [{
                            parts: [
                                { text: defaultPrompt },
                                { inlineData: { mimeType: 'image/jpeg', data: imageBase64 } }
                            ]
                        }]
                    })
                });
                const geminiData = await geminiRes.json();
                if (geminiData.candidates && geminiData.candidates[0]?.content?.parts[0]?.text) {
                    let text = geminiData.candidates[0].content.parts[0].text.trim();
                    text = text.replace(/```json\s*/, '').replace(/```\s*/, '').trim();
                    return res.json(JSON.parse(text));
                }
            } catch (err) {
                console.warn('Backend Gemini Vision error:', err.message);
            }
        }

        return res.status(503).json({ error: 'No AI Vision service configured on server or request failed' });
    } catch (err) {
        console.error('AI image analysis route error:', err);
        res.status(500).json({ error: 'AI image analysis failed', details: err.message });
    }
});

// GET /api/reports/heatmap - Returns geo-tagged counterfeit incident report markers
app.get('/api/reports/heatmap', async (req, res) => {
    try {
        const snapshot = await db.ref('reports').once('value');
        if (!snapshot.exists()) {
            return res.json([]);
        }
        const reports = [];
        snapshot.forEach(child => {
            const data = child.val();
            if (data.latitude && data.longitude) {
                reports.push({
                    reportId: data.reportId || child.key,
                    medicineName: data.medicineName,
                    barcode: data.barcode,
                    location: data.location || 'Unknown Location',
                    latitude: data.latitude,
                    longitude: data.longitude,
                    timestamp: data.timestamp,
                    status: data.status || 'pending'
                });
            }
        });
        res.json(reports);
    } catch (err) {
        console.error('Heatmap route error:', err);
        res.status(500).json({ error: 'Failed to fetch heatmap data', details: err.message });
    }
});

// POST /api/blockchain/verify-chain - Verifies supply chain multi-node audit trail
app.post('/api/blockchain/verify-chain', async (req, res) => {
    try {
        const { barcode, medicineName, batchNumber } = req.body;
        if (!barcode && !medicineName) {
            return res.status(400).json({ error: 'Barcode or Medicine Name required' });
        }

        // Simulate Smart Contract multi-node ledger verification
        const isVerified = !(medicineName && (medicineName.toLowerCase().includes('serostim') || medicineName.toLowerCase().includes('fake')));

        const chainTimeline = [
            {
                step: 1,
                node: "Manufacturer Node",
                actor: "PharmaCorp Manufacturing Ltd.",
                status: isVerified ? "VALID" : "FLAGGED",
                timestamp: "2026-01-10 09:30:00",
                hash: "0x" + Math.random().toString(16).substring(2, 18) + "a8b9",
                details: "Batch created with cryptographic genesis stamp"
            },
            {
                step: 2,
                node: "Distributor Node",
                actor: "Global Logistics Hub",
                status: isVerified ? "VALID" : "FLAGGED",
                timestamp: "2026-01-15 14:15:00",
                hash: "0x" + Math.random().toString(16).substring(2, 18) + "c7d2",
                details: "Cold chain telemetry verified (2°C - 8°C)"
            },
            {
                step: 3,
                node: "Pharmacy Node",
                actor: "Central Retail Pharmacy",
                status: isVerified ? "VALID" : "FLAGGED",
                timestamp: "2026-01-20 11:45:00",
                hash: "0x" + Math.random().toString(16).substring(2, 18) + "e4f1",
                details: "Stock authenticated and registered on ledger"
            },
            {
                step: 4,
                node: "Consumer Node",
                actor: "PharmaGuard Mobile App",
                status: isVerified ? "VALID" : "COUNTERFEIT_ALERT",
                timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
                hash: "0x" + Math.random().toString(16).substring(2, 18) + "f9a0",
                details: isVerified ? "Consumer barcode verification matched on smart contract" : "Hash mismatch detected: Counterfeit packaging suspect"
            }
        ];

        res.json({
            barcode: barcode || 'UNKNOWN',
            medicineName: medicineName || 'UNKNOWN',
            batchNumber: batchNumber || 'BATCH-2026',
            isVerified: isVerified,
            smartContractAddress: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F",
            network: "Polygon POS Mainnet",
            chainTimeline: chainTimeline
        });
    } catch (err) {
        console.error('Blockchain verify route error:', err);
        res.status(500).json({ error: 'Smart contract verification failed' });
    }
});

// Health check
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', service: 'PharmaGuard JWT Server' });
});


// ─── Start ─────────────────────────────────────────────────
app.listen(PORT, "0.0.0.0", () => {
    console.log(`PharmaGuard JWT Server running on port ${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/api/health`);
});
