package com.sprout.friendfinder.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.spongycastle.openssl.PEMParser;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

/**
 * This contains both the PKCS signature and generated secret, as well as the matching plaintext id's
 * 
 * It stores this information in a DB and interfaces directly with the ATWPSI protocol
 * @author skyf
 *
 */
// Arguably this should be two separate objects (but this results in a lot of repeated information)
@Table(name = "authorizations")
public class AuthorizationObject extends Model implements Serializable {
  private static final long serialVersionUID = -8723688620635455716L;

  private static final String TAG = AuthorizationObject.class.getSimpleName();
  private static final boolean D = true;

  /**
   * Number of seconds a signature should be considered valid
   */
  private static final int VALID_SECONDS = 7*24*60*60;

  /**
   * Location of servers installed certificate
   */
  private static String certFilename = "server_cert.crt";

  // TODO: Include a valid_until column
  //  this column needs to be calculated form the authorization

  // TODO: When support for many authorizations is added include a
  //   timestamp of when this was last used (if ever)

  @Column
  private BigInteger R;
  
  @Column
  private BigInteger R2; // for psi-ca protocol

  @Column
  private String auth;

  @Column
  private transient String authIdentity; // Transient used to leave out of serialization

  private X509Certificate cert;

  // The original order of the calculated input
  @Column
  private transient ArrayList<String> orderedInput;
  
  public enum AuthorizationObjectType {
	  PSI, PSI_CA, PSI_CA_DEP;
  }
  
  @Column
  protected AuthorizationObjectType type = AuthorizationObjectType.PSI; // default type is psi
  
  public static AuthorizationObject getAvailableAuth(Context context, AuthorizationObjectType type) throws CertificateException, IOException {
    // Return any auth for now, since we don't batch authorizations
    AuthorizationObject auth = new Select().from(AuthorizationObject.class).where("type=?",type).executeSingle();
    Log.i(TAG, "Load cert for type: "+type);
    auth.loadCert(context);
    return auth;
  }

  public AuthorizationObject() {
    super();

    // TODO: Figure out how to load a cert here without a context.
    // Otherwise using Model functions (or this constructor) may result in undefined behavior
  }

  // TODO: Do we need to override load instead of default constructor?

  public AuthorizationObject(Context ctx) throws IOException, CertificateException {
    super();
    loadCert(ctx);
  }

  public AuthorizationObject(Context ctx, BigInteger R, BigInteger R2, String auth) throws IOException, CertificateException {
    super();

    this.auth = auth;
    this.R = R;
    this.R2 = R2;
    loadCert(ctx);
  }


  public AuthorizationObject(Context ctx, BigInteger R, String auth) throws IOException, CertificateException {
    super();

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
    super(); 

    this.R = null;
    this.cert = other.cert;
    this.auth = auth;
  }
  
  public AuthorizationObject(AuthorizationObject other, String auth, String authIdentity) {
    super(); 

    this.R = null;
    this.cert = other.cert;
    this.auth = auth;
    this.authIdentity = authIdentity;
  }

  public AuthorizationObject(Context ctx, String response, AuthorizationObjectType type) throws JSONException, IOException, CertificateException {
    super();
    this.type = type;
    loadCert(ctx);
    JSONObject jObject = new JSONObject(response);

    JSONObject msg = jObject.getJSONObject("psi_message");
    
    String Rstring = (String) msg.get("secret");
    Log.d(TAG, "Rstring: "+Rstring);
    String[] RstringList = Rstring.split(" ");

    R = decode( RstringList[0] );
    if(type.equals(AuthorizationObjectType.PSI_CA_DEP)) {
      R2 = decode( RstringList[1] );
      Log.d(TAG, "R1: "+RstringList[0]+"   R2: "+RstringList[1]);
    }

    auth = msg.getString("signed_message");

    if (jObject.has("connections")) {
      JSONArray connections = jObject.getJSONArray("connections");

      if(D) Log.d(TAG, "Initializing orderedInput");
      orderedInput = new ArrayList<String>();

      for (int i=0; i < connections.length(); i++ ) {
        orderedInput.add( connections.getString(i) );
      }
    } else {
      if(D) Log.d(TAG, "Connections not present in top level json");
    }

    if (jObject.has("signed_identity")) {
      authIdentity = jObject.getString("signed_identity");
    } else {
      if(D) Log.d(TAG, "Signed identity not present.");
    }
    if(D) Log.d(TAG, "Successfully dl auth obj");
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
  
  private boolean verify(String authString) {
    if (authString == null) {
      throw new NullPointerException ("Verify called on an uninitialized auth object.");
    }
    // We may want a way to verify arbitrary data. In case the signature is detached. 
    // for now we don't bother

    // TODO: Validate the time
    try {
      @SuppressWarnings("resource")
      ContentInfo ci = (ContentInfo) new PEMParser(new StringReader(authString)).readObject();
      CMSSignedData cms = new CMSSignedData(ci);

      // Retrieve the first signer
      SignerInformation signer = (SignerInformation)cms.getSignerInfos().getSigners().iterator().next();

      // Verify
      //signer.verify(new BcRSASignerInfoVerifierBuilder(new DefaultDigestAlgorithmIdentifierFinder(), new BcDigestCalculatorProvider()).build(cert))));
      // Another alternative. The difference is in the providers
      boolean result = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert));

      return result;
    } catch (Exception e) {
      Log.e(TAG, "Authorization processing error", e);
      return false;
    }
  }
  
  public boolean verifyIdentity() {
    return verify(authIdentity);
  }

  /**
   * 
   * @return validity of this auth
   */
  public boolean verify() {
    return verify(auth);
  }

  /* These are unused, but left in order to make use of the serialization api at a later date
   *       To do this we need to change the signatures to private
  public void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    out.writeObject(this.R);
    out.writeObject(this.auth);
    out.writeObject(this.orderedInput);
    out.writeObject(this.authIdentity);
  }

  @SuppressWarnings("unchecked")
  public AuthorizationObject readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.R = (BigInteger) in.readObject();
    this.auth = (String) in.readObject();
    this.orderedInput = (ArrayList<String>) in.readObject();
    this.authIdentity = (String) in.readObject();
    return this;
  } */

  public BigInteger getR() {
    return this.R;
  }

  public String getAuth() {
    return this.auth;
  }
  
  public AuthorizationObjectType getType() {
    return this.type;
  }

  public List<String> getOriginalOrder() {
    return orderedInput;
  }
  
  private String decodeData(String encodedData) {

    // decode the encoded data object if necessary

    try {
      @SuppressWarnings("resource")
      ContentInfo ci = (ContentInfo) new PEMParser(new StringReader(encodedData)).readObject();
      CMSSignedData cms = new CMSSignedData(ci);


      ByteArrayOutputStream out = new ByteArrayOutputStream();

      cms.getSignedContent().write(out);

      byte [] bytes = out.toByteArray();

      // the data should be a string that is a series of base64 encodded bigintegers seperated by spaces
      // So the following *shoudl* work

      String base64 = new String(bytes);

      return base64;
    } catch (CMSException e) {
      Log.e(TAG, "Authorization processing error" , e);
      return null;
    } catch (IOException e) {
      Log.e(TAG, "Error processing content" , e);
      return null;
    }
  }
  
  public String getDecodedIdentity() {
    return decodeData(authIdentity);
  }

  public List<BigInteger> getData () {
    String base64 = decodeData(auth);
    if(base64==null) return null;

    List<BigInteger> ret = new ArrayList<BigInteger>();

    for( String bn : base64.split(" ") ) {
      ret.add( decode(bn) );
    }

    return ret;
  }

  public String getIdentity() {
    return authIdentity;
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

  public BigInteger getR2() {
    return R2;
  }
}
