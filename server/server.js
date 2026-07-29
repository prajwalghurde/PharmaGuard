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

// Health check
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', service: 'PharmaGuard JWT Server' });
});

// ─── Start ─────────────────────────────────────────────────
app.listen(PORT, "0.0.0.0", () => {
    console.log(`PharmaGuard JWT Server running on port ${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/api/health`);
});
