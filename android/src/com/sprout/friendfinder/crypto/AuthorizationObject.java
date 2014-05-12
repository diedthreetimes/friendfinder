/*
 * Ron: The CA's public key (N, e), the random number Rc as well as the authorization of the set of contacts by CA are put into this object in order to store it on the user's device and read it later on (during offline phase) when the PSI shall be done
 */

package com.sprout.friendfinder.crypto;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

public class AuthorizationObject implements Serializable {
  private static final long serialVersionUID = -8723688620635455716L;

  private static final String TAG = AuthorizationObject.class.getSimpleName();
  private static final boolean D = true;
  

  /**
   * Number of seconds a signature should be considered valid
   */
  private static final int VALID_SECONDS = 7*24*60*60;

  private BigInteger R;
  private String auth;
  
  private X509Certificate cert;

  private static String defaultFilename = "authorization";
  private static String certFilename = "server_cert.crt"; // Pem should also work if needed

  // TODO: There should be a static load function instead of an empty constructor
  public AuthorizationObject(Context ctx) throws IOException, CertificateException {
    loadCert(ctx);
  }
  
  public AuthorizationObject(Context ctx, BigInteger R, String auth) throws IOException, CertificateException {
    this.auth = auth;
    this.R = R;
    loadCert(ctx);
  }
  
  /**
   * Use another authorizations certificate to validate auth
   * @param other
   * @param auth
   */
  public AuthorizationObject(AuthorizationObject other, String auth) {
    this.R = null;
    this.cert = other.cert;
    this.auth = auth;
  }
  
  public AuthorizationObject(Context ctx, String response) throws JSONException, IOException, CertificateException {
    loadCert(ctx);
    JSONObject jObject = new JSONObject(response);
    
    auth = jObject.getString("psi_message");
    R = decode( jObject.getString("secret") );
    
  }
  
  private void loadCert(Context context) throws IOException, CertificateException {
    Resources resources = context.getResources();
    AssetManager assetManager = resources.getAssets();
    InputStream inputStream = assetManager.open(certFilename);
    
    CertificateFactory factory = CertificateFactory.getInstance("X.509");  
    cert = (X509Certificate) factory.generateCertificate(inputStream); 
    
  }


  public static boolean verify(Context context, String auth) {
    try {
      return new AuthorizationObject(context, null, auth).verify();
    } catch (Exception e) {
      Log.e(TAG, "Certificate could not be parsed", e);
      return false;
    }
  }
  
  /**
   * 
   * @return validity of this auth
   */
  public boolean verify() {
    if (auth == null) {
      throw new NullPointerException ("Verify called on an uninitialized auth object.");
    }
    // We may want a way to verify arbitrary data. In case the signature is detached. 
    // for now we don't bother
    
    // TODO: Validate the time
    try {
      CMSSignedData cms = new CMSSignedData(Base64.decode(auth.getBytes(), Base64.DEFAULT));
    

      // Retreive the first signer
      SignerInformation signer = (SignerInformation)cms.getSignerInfos().getSigners().iterator().next();

      // Verify
      //signer.verify(new BcRSASignerInfoVerifierBuilder(new DefaultDigestAlgorithmIdentifierFinder(), new BcDigestCalculatorProvider()).build(cert))));
      // Another alternative. The difference is in the providers
      boolean result = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert));


      System.out.println("Verified: "+result);


      return result;
    } catch (Exception e) {
      Log.e(TAG, "Authorization processing error", e);
      return false;
    }
  }

  public void save(Context context) throws IOException {
    save(context, defaultFilename);
  }

  public void save(Context context, String filename) throws IOException {
    FileOutputStream fileOutput = context.openFileOutput(filename, Context.MODE_PRIVATE);
    ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
    writeObject(objectOutput);
    objectOutput.close();
    Log.d(TAG, "Authorization saved");
  }

  public void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    out.writeObject(this.R);
    out.writeObject(this.auth);
  }

  public AuthorizationObject readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.R = (BigInteger) in.readObject();
    this.auth = (String) in.readObject();
    return this;
  }

  public BigInteger getR() {
    return this.R;
  }

  public String getAuth() {
    return this.auth;
  }

  public List<BigInteger> getData () {
    // decode the encoded data object if necessary
    
    try {
      CMSSignedData cms = new CMSSignedData(Base64.decode(auth.getBytes(), Base64.DEFAULT));
 
      
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      
      cms.getSignedContent().write(out);
      
      byte [] bytes = out.toByteArray();
      
      // TODO: !!!!!!
      
      
      return null;
    } catch (CMSException e) {
      Log.e(TAG, "Authorization processing error" , e);
      return null;
    } catch (IOException e) {
      Log.e(TAG, "Error processing content" , e);
      return null;
    }
  }
  
  /**
   * Decodes big integers encoded at the server
   */
  private static BigInteger decode( String bn ) {
    byte[] hex = Base64.decode(bn.getBytes(), Base64.DEFAULT);

    
    byte[] array = new byte[hex.length + 1];
    ByteBuffer bbuf = ByteBuffer.wrap(array);
    bbuf.put((byte)0);
    bbuf.put(hex);
    
    return new BigInteger(array);
  }
}
