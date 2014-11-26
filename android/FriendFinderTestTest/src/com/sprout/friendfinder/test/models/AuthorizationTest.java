package com.sprout.friendfinder.test.models;

import java.io.IOException;
import java.security.cert.CertificateException;

import org.json.JSONException;

import com.activeandroid.ActiveAndroid;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.test.helpers.DatabaseHelper;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

public class AuthorizationTest extends AndroidTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    RenamingDelegatingContext context = new RenamingDelegatingContext(
        getContext(), "test_");
    ActiveAndroid.initialize(context);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    ActiveAndroid.dispose();
  }
  
  public void testLoad() throws CertificateException, IOException, JSONException {
    AuthorizationObject origin = new AuthorizationObject(getContext(), DatabaseHelper.sampleAuthResponse());
    origin.save();
    
    assertNotNull(origin.getR());
    assertNotNull(origin.getOriginalOrder());
    assertNotNull(origin.getIdentity());
    
    AuthorizationObject auth = AuthorizationObject.getAvailableAuth(getContext());
    
    assertNotNull(auth.getR());
    assertNotNull(auth.getOriginalOrder());
    assertNotNull(auth.getIdentity());
  }
}
