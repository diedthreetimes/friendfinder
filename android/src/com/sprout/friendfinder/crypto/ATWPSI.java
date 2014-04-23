package com.sprout.friendfinder.crypto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.AbstractPSIProtocol;


// Authorized Two Way Psi 
public class ATWPSI extends AbstractPSIProtocol<String, Void, List<String>> {
	private static String TAG = "TwoWayPSI";
	
    //these variables are needed for the PSI protocol -> they should rather be put to the message handler class as they are used there...
    BigInteger Ra, Rb, N, e;
    List<BigInteger> zis;
    List<BigInteger> yis;

    AuthorizationObject authObj;
    
    //keep track of state the PSI protocol currently is in
    BigInteger peerAuth;
    List<BigInteger> T;
    List<BigInteger> T2;
    
    public ATWPSI(CommunicationService s, AuthorizationObject authObject) {
		super(TAG, s, true); // This relies on the fact that the protocol is mirrored. 
		
		authObj = authObject;
	}
    
	public ATWPSI(String testName, CommunicationService s, AuthorizationObject authObject) {
		super(testName, s, true); // This relies on the fact that the protocol is mirrored. 
		
		authObj = authObject;
	}
	
	public ATWPSI(String testName, CommunicationService s, AuthorizationObject authObject, boolean client) {
		super(testName, s, client); // This relies on the fact that the protocol is mirrored. 
		
		authObj = authObject;
	}
    
    // TODO: We need a way to set the authorization parameters.
    //   This is probably easiest to do in a function
	
    // TODO: Make this protocol pipelined
    
	@Override
	protected List<String> conductClientTest(CommunicationService s, String... input) {
		// As of now, the protocol is mirrored for client and server
		return conductServerTest(s, input);
	}

	@Override
	protected List<String> conductServerTest(CommunicationService s, String... input) {
		
		Log.d(TAG, "STARTING TEST");
		
		// TODO: confirm that N is not simply the same as p
		//  We need to reconcile this for the hash function to work properly
		
		// N = p;
		Ra = authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts
		N = authObj.getN();
		e = authObj.gete();

		zis = new ArrayList<BigInteger>();
		for (String inp : input) {
			BigInteger zi = hash(inp).modPow(Ra, N); // TODO: This set needs to be part of the authorization.
			zis.add(zi);		
		}

		//send zis + auth to peer:
		//send number of zis first, so that peer knows how many BigInt-Reads it has to conduct:
		s.write(String.valueOf(zis.size()));
		
	  	//A sends zis to B
	  	for (int i = 0; i < zis.size(); i++) {          		  
	  		s.write(zis.get(i));
	  	}
	  	s.write(authObj.getAuth());
	  	
		//A receives B's yis
		List<BigInteger> peerSet;
	  	int peerSetSize = Integer.parseInt(s.readString());
	  	peerSet = new ArrayList<BigInteger>();

	  	while(peerSet.size() != peerSetSize) {
	  		peerSet.add(s.readBigInteger());
	  	}

	  	//A received B's auth:
	  	peerAuth = s.readBigInteger();
	  	  
	  	//A checks B's auth:
	  	
	  	// TODO: double check this logic
	  	// TODO: The sig should be in a different group! Refactor this into a different class
	  	BigInteger verification = peerAuth.modPow(e, N);
	  	BigInteger toVerify = BigInteger.ONE;
	  	for (BigInteger toverifyi : peerSet) {
	  		toVerify = toVerify.multiply(toverifyi).mod(N);
	  	}
	  	  
	  	toVerify = hash(toVerify.toByteArray(), (byte)0).mod(N);
	  	if (verification.equals(toVerify)) {
	  		Log.d(TAG, "A: Contact set verification succeeded");
	  	}
	  	else {
	  		Log.d(TAG, "A: Contact set verification failed");
	  		Log.d(TAG, "received verification tag: " + verification.toString());
	  		Log.d(TAG, "computed verification tag: " + toVerify.toString());
	  	}


	  	//A performs computations on yis and sends them to B:
	  	T = new ArrayList<BigInteger>();
	  	for (BigInteger t1i : peerSet) {
	  		t1i = t1i.modPow(Ra, N);
	  		T.add(t1i);

	  		s.write(t1i);
	  	}

	  	//prepare for receiving B's computed tags
	  	T2 = new ArrayList<BigInteger>();
	  	
	  	//A receives B's computed tags:
	  	for (int i = 0; i < zis.size(); i++) {
	  		T2.add(s.readBigInteger());
	  	}

	  	//output result set:
	  	List<String> result = new ArrayList<String>();
	  	
	  	for (int l = 0; l < T2.size(); l++) {
	  		for (int l2 = 0; l2 < T.size(); l2++) {
	  			if (T.get(l).equals(T2.get(l2))) {
	  				result.add(input[l]);
	  			}
	  		}
	  	}

	  	Log.d(TAG, "Found " + result.size() + " common friends");

//	  		String[] inCommon = new String[inCommon2.size()];
//	  		for (int l = 0; l < inCommon2.size(); l++) {
//	  			inCommon[l] = inCommon2.get(l);
//	  		}
//	  		mTarget.get().currentPeer.setCommonFriends(inCommon);

	  		//now show the result of the PSI to the user in a dialog:
//	  		CommonFriendsDialogFragment newFragment = new CommonFriendsDialogFragment(); //DialogFragment
//	  		newFragment.setCurrentPeer(currentPeer);
//	  		newFragment.show(getFragmentManager(), "check-common-friends");
//
//	  		//the user's response to the "become friends" question is handled in the methods below

	  		//mTarget.get().state = 1; 
	  	  
		return result;
	}
}
