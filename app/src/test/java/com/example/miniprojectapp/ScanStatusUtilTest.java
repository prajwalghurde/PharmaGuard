package com.example.miniprojectapp;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScanStatusUtilTest {

    @Test
    public void isSuccessStatus_acceptsBlockchainVerified() {
        assertTrue(ScanStatusUtil.isSuccessStatus("Blockchain Verified"));
    }

    @Test
    public void isSuccessStatus_acceptsLegacyVerifiedLabels() {
        assertTrue(ScanStatusUtil.isSuccessStatus("Verified"));
        assertTrue(ScanStatusUtil.isSuccessStatus("AI Verified"));
    }

    @Test
    public void isSuccessStatus_rejectsCounterfeitLabels() {
        assertFalse(ScanStatusUtil.isSuccessStatus("Tampered Record"));
        assertFalse(ScanStatusUtil.isSuccessStatus("Not Found"));
        assertFalse(ScanStatusUtil.isSuccessStatus(null));
    }

    @Test
    public void isDangerStatus_acceptsCounterfeitLabels() {
        assertTrue(ScanStatusUtil.isDangerStatus("Tampered Record"));
        assertTrue(ScanStatusUtil.isDangerStatus("Not Found"));
        assertTrue(ScanStatusUtil.isDangerStatus("Counterfeit"));
    }
}
