package com.example.miniprojectapp;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented tests for SessionManager - SharedPreferences session persistence.
 * Must run on a real device or emulator (uses Android context).
 */
@RunWith(AndroidJUnit4.class)
public class SessionManagerTest {

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        sessionManager = new SessionManager(ApplicationProvider.getApplicationContext());
        // Always start with a clean session
        sessionManager.clearSession();
    }

    @After
    public void tearDown() {
        sessionManager.clearSession();
    }

    // ─── Initial State ─────────────────────────────────────────

    @Test
    public void sessionManager_initialState_notLoggedIn() {
        assertFalse("Fresh session should not be logged in", sessionManager.isLoggedIn());
    }

    @Test
    public void sessionManager_initialState_tokenEmpty() {
        assertEquals("", sessionManager.getToken());
    }

    @Test
    public void sessionManager_initialState_uidEmpty() {
        assertEquals("", sessionManager.getUid());
    }

    @Test
    public void sessionManager_initialState_emailEmpty() {
        assertEquals("", sessionManager.getEmail());
    }

    @Test
    public void sessionManager_initialState_nameEmpty() {
        assertEquals("", sessionManager.getName());
    }

    @Test
    public void sessionManager_initialState_phoneEmpty() {
        assertEquals("", sessionManager.getPhone());
    }

    // ─── Save Session ──────────────────────────────────────────

    @Test
    public void saveSession_setsLoggedInTrue() {
        sessionManager.saveSession("token123", "uid001", "test@test.com", "Test User", "9999999999");
        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void saveSession_persistsToken() {
        sessionManager.saveSession("my_jwt_token", "uid001", "a@b.com", "Name", "");
        assertEquals("my_jwt_token", sessionManager.getToken());
    }

    @Test
    public void saveSession_persistsUid() {
        sessionManager.saveSession("tok", "user_abc123", "a@b.com", "Name", "");
        assertEquals("user_abc123", sessionManager.getUid());
    }

    @Test
    public void saveSession_persistsEmail() {
        sessionManager.saveSession("tok", "uid", "john@pharmaguard.com", "John", "");
        assertEquals("john@pharmaguard.com", sessionManager.getEmail());
    }

    @Test
    public void saveSession_persistsName() {
        sessionManager.saveSession("tok", "uid", "email@x.com", "John Doe", "");
        assertEquals("John Doe", sessionManager.getName());
    }

    @Test
    public void saveSession_persistsPhone() {
        sessionManager.saveSession("tok", "uid", "email@x.com", "Jane", "9876543210");
        assertEquals("9876543210", sessionManager.getPhone());
    }

    @Test
    public void saveSession_emptyPhone_persistsEmpty() {
        sessionManager.saveSession("tok", "uid", "email@x.com", "Jane", "");
        assertEquals("", sessionManager.getPhone());
    }

    // ─── Overwrite Session ─────────────────────────────────────

    @Test
    public void saveSession_twice_latestValuePersists() {
        sessionManager.saveSession("token_v1", "uid1", "old@test.com", "Old Name", "1111111111");
        sessionManager.saveSession("token_v2", "uid2", "new@test.com", "New Name", "2222222222");

        assertEquals("token_v2", sessionManager.getToken());
        assertEquals("uid2", sessionManager.getUid());
        assertEquals("new@test.com", sessionManager.getEmail());
        assertEquals("New Name", sessionManager.getName());
        assertEquals("2222222222", sessionManager.getPhone());
        assertTrue(sessionManager.isLoggedIn());
    }

    // ─── Clear Session ─────────────────────────────────────────

    @Test
    public void clearSession_afterSave_setsLoggedInFalse() {
        sessionManager.saveSession("tok", "uid", "a@b.com", "Name", "");
        sessionManager.clearSession();
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void clearSession_afterSave_tokenIsEmpty() {
        sessionManager.saveSession("tok", "uid", "a@b.com", "Name", "");
        sessionManager.clearSession();
        assertEquals("", sessionManager.getToken());
    }

    @Test
    public void clearSession_afterSave_emailIsEmpty() {
        sessionManager.saveSession("tok", "uid", "test@pharmaguard.com", "Name", "");
        sessionManager.clearSession();
        assertEquals("", sessionManager.getEmail());
    }

    @Test
    public void clearSession_withoutSave_doesNotThrow() {
        // Should not throw even if nothing was saved
        sessionManager.clearSession();
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void clearSession_calledTwice_doesNotThrow() {
        sessionManager.saveSession("tok", "uid", "a@b.com", "Name", "");
        sessionManager.clearSession();
        sessionManager.clearSession(); // second call should be safe
        assertFalse(sessionManager.isLoggedIn());
    }

    // ─── JWT Token Format ──────────────────────────────────────

    @Test
    public void saveSession_longJwtToken_persistsCorrectly() {
        String longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                           ".eyJ1aWQiOiJ1c2VyXzEiLCJlbWFpbCI6InRlc3RAdGVzdC5jb20iLCJuYW1lIjoiVGVzdCJ9" +
                           ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        sessionManager.saveSession(longToken, "uid1", "test@test.com", "Test", "");
        assertEquals(longToken, sessionManager.getToken());
    }
}
