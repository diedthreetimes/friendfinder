package com.sprout.friendfinder.crypto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.crypto.AbstractPSIProtocol;
import com.sprout.finderlib.communication.BluetoothService;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.social.CurrentPeer;
import com.sprout.friendfinder.social.ProfileObject;
import com.sprout.friendfinder.ui.CommonFriendsDialogFragment;

// Authorized Two Way Psi 
public class ATWPSI extends AbstractPSIProtocol {

	private static String TAG = "TwoWayPSI";
	
    //these variables are needed for the PSI protocol -> they should rather be put to the message handler class as they are used there...
    CurrentPeer currentPeer;
    BigInteger Ra, Rb, N, e;
    List<BigInteger> zis;
    List<BigInteger> yis;

    AuthorizationObject authObj;
    
    //keep track of state the PSI protocol currently is in
    BigInteger peerAuth;
    List<BigInteger> T;
    List<BigInteger> T2;
    
    public void setAuthorization(AuthorizationObject auth) {
    	authObj = auth;
    }
    
    // TODO: We need a way to set the authorization parameters.
    //   This is probably easiest to do in a function
	
    // TODO: Make this protocol pipelined
    
	@Override
	protected Collection<String> conductClientTest(CommunicationService s, Collection<String> input) {
		// As of now, the protocol is mirrored for client and server
		return conductServerTest(s, input);
	}

	@Override
	protected Collection<String> conductServerTest(CommunicationService s, Collection<String> input) {
		
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
		
		// state 2
		

	  	//A sends zis to B
	  	for (int i = 0; i < zis.size(); i++) {          		  
	  		s.write(zis.get(i));
	  	}
	  	
	  	
		//A receives B's number of yis
		List<BigInteger> peerSet;
	  	int peerSetSize = Integer.parseInt(s.readString());
	  	peerSet = new ArrayList<BigInteger>();
	  	  
	  	// state = 6; 

	  	// TODO: Add methods for reading and writing collections
	  	
	  	//A receives B's yis
	  	while(peerSet.size() != peerSetSize) {
	  		peerSet.add(s.readBigInteger());
	  	}
	  	
	  	//A sends auth to B:

	  	s.write(authObj.getAuth());
	  	
	  	// state = 8;

	  	//A received B's auth:
	  	peerAuth = s.readBigInteger();
	  	  
	  	//A checks B's auth:
	  	
	  	// TODO: double check this logic
	  	// TODO: The sig should be in a different group!
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

	  	// state = 10
	  	
	  	//A receives B's computed tags:
	  	for (int i = 0; i < zis.size(); i++) {
	  		T2.add(s.readBigInteger());
	  	}

	  	//output result set:
	  	Collection<String> result = new ArrayList<String>();
	  	
	  	String[] inp = input.toArray(new String[0]);
	  	List<String> inCommon2 = new ArrayList<String>();
	  	for (int l = 0; l < T2.size(); l++) {
	  		for (int l2 = 0; l2 < T.size(); l2++) {
	  			if (T.get(l).equals(T2.get(l2))) {
	  				result.add(inp[l]);
	  			}
	  		}
	  	}
	  	
	  	// Collection<String> result = T2;
	  	


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

	
	// Bellow are the old steps of the PSI protocol, conducted inside a handler.
    
    
/*    if (mTarget.get().state == 1) { //readMessage.startsWith("1")
  /*	  //Toast.makeText(target.getApplicationContext(), "You are connected to " 
  		//	  + readMessage + " now. This does not mean that you are already Linkedin contacts.", Toast.LENGTH_SHORT).show(); //.substring(1)
  	  mTarget.get().loadProfileFromFile(); //need to get our own profile information first
  	  //set the current peer:
  	  
  	  mTarget.get().currentPeer = new CurrentPeer(); //currentPeer could be moved to this class... 
  	  //currentPeer.setId();
  	  //mTarget.get().currentPeer.setName(mTarget.get().myProfile.getFirstName() + " " + mTarget.get().myProfile.getLastName());
  	  mTarget.get().currentPeer.setName(readMessage);
  	  
  	  //"2" + 
  	  
  	  mTarget.get().mMessageService.write(mTarget.get().myProfile.getFirstName() + " " + mTarget.get().myProfile.getLastName()); //we should also provide the Linkedin ID here so that the connecting can be done later on
  	  
  	  
  	  
  	  
  	  
  	  //set my own state for the next phase to the next level
  	  mTarget.get().state = 3;
    }
   
    else if (mTarget.get().state == 2) { //readMessage.startsWith("2")
  	  //Toast.makeText(target.getApplicationContext(), "You are connected to " 
  	//		  + readMessage + " now. This does not mean that you are already Linkedin contacts.", Toast.LENGTH_SHORT).show(); //.substring(1)
  	
  	 /* 
  	  //set the current peer:
  	  mTarget.get().currentPeer = new CurrentPeer(); //currentPeer could be moved to this class... 
  	  //currentPeer.setId();
  	  //mTarget.get().currentPeer.setName(mTarget.get().myProfile.getFirstName() + " " + mTarget.get().myProfile.getLastName());
  	  mTarget.get().currentPeer.setName(readMessage);*//*
  	  
  	  
  	//do the PSI with the current peer now:
	    //get my own contactList (as we are offline now, we need to load the saved contact list from the file)
//	    mTarget.get().loadContactsFromFile();
//	    
//	    //load authorization data from file as well:
//	    mTarget.get().loadAuthorizationFromFile();
//	    
	    
//	    mTarget.get().Ra = mTarget.get().authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts
//	    mTarget.get().N = mTarget.get().authObj.getN();
//	    mTarget.get().e = mTarget.get().authObj.gete();
//	    mTarget.get().zis = new ArrayList<BigInteger>();
//	    for (ProfileObject pro : mTarget.get().contactList) { //for all elements in my own contact list
//	    	mTarget.get().zis.add(mTarget.get().hash(pro.getId().getBytes(), (byte)0).modPow(mTarget.get().Ra, mTarget.get().N));
//	    }
//	    
//	    
//	    //send zis + auth to peer:
//	      //send number of zis first, so that peer knows how many BigInt-Reads it has to conduct:
//	    Integer s = mTarget.get().zis.size();
//	    
//	    
//	    mTarget.get().mMessageService.write(s.toString());
	      
	      
//	    mTarget.get().state = 4;
    
    }
                    

    else if (mTarget.get().state == 3) {
//  	 //B receives A's number of zis
//  	  mTarget.get().peerSetSize = Integer.parseInt(readMessage);
//  	  mTarget.get().peerSet = new ArrayList<BigInteger>();
//  	  
//  	  
//  	  //B initializes itself
//  	  mTarget.get().loadContactsFromFile();
//  	    
//  	  //load authorization data from file as well:
//  	  mTarget.get().loadAuthorizationFromFile();
//  	    
//  	  mTarget.get().Rb = mTarget.get().authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts
//
//
//  	  mTarget.get().N = mTarget.get().authObj.getN();
//  	  mTarget.get().e = mTarget.get().authObj.gete();
//  	  mTarget.get().yis = new ArrayList<BigInteger>();
//  	  for (ProfileObject pro : mTarget.get().contactList) { //for all elements in my own contact list
//  	    mTarget.get().yis.add(mTarget.get().hash(pro.getId().getBytes(), (byte)0).modPow(mTarget.get().Rb, mTarget.get().N));
//  	  }
//  	    
//  	  //B sends number of yis to A:
//  	  Integer s = mTarget.get().yis.size();
//  	  
//  	  mTarget.get().mMessageService.write(s.toString());
//  	    
//  	    
//  	  mTarget.get().state = 5;
  
    }
    
    else if (mTarget.get().state == 4) {
//  	  //A receives B's number of yis
//  	  mTarget.get().peerSetSize = Integer.parseInt(readMessage);
//  	  mTarget.get().peerSet = new ArrayList<BigInteger>();
//  	  
//  	  //A sends zis to B
//  	  for (int i = 0; i < mTarget.get().zis.size(); i++) {          		  
//  		  mTarget.get().mMessageService.write(mTarget.get().zis.get(i));
//	      }
//  	  
//  	  mTarget.get().state = 6;
  	  
    }
    
    else if (mTarget.get().state == 5) {
//  	  //B receives A's zis
//  	  mTarget.get().peerSet.add(new BigInteger(readBuf));
//	  		  
//	  	  if (mTarget.get().peerSet.size() == mTarget.get().peerSetSize) { //i.e. received all elements
//	  		//B sends its yis to A:
//      	  for (int i = 0; i < mTarget.get().yis.size(); i++) {
//      		  mTarget.get().mMessageService.write(mTarget.get().yis.get(i));
//      	  }
//	  		  
//	  		  mTarget.get().state = 7;
//	  	  }
//	  	           	  	  
    }
    
    else if (mTarget.get().state == 6) {
//  	  //A receives B's yis
//  	  mTarget.get().peerSet.add(new BigInteger(readBuf));
//  	  
//  	  if (mTarget.get().peerSet.size() == mTarget.get().peerSetSize) {
//  		//A sends auth to B:
//      	  
//  		  mTarget.get().mMessageService.write(mTarget.get().authObj.getAuth());
//  		  
//  		  mTarget.get().state = 8;
//  	  }
  	  
    }
    
    else if (mTarget.get().state == 7) {
  	  //B receives A's auth:
  	  mTarget.get().peerAuth = new BigInteger(readBuf);
  	  
  	  //B checks A's auth:
  	  BigInteger verification = mTarget.get().peerAuth.modPow(mTarget.get().e, mTarget.get().N);
  	  BigInteger toVerify = BigInteger.ONE;
  	  for (BigInteger toverifyi : mTarget.get().peerSet) {
  		  toVerify = toVerify.multiply(toverifyi).mod(mTarget.get().N);
  	  }
  	  
  	  toVerify = mTarget.get().hash(toVerify.toByteArray(), (byte)0).mod(mTarget.get().N);
  	  if (verification.equals(toVerify)) {
  		  Log.d(TAG, "B: Contact set verification succeeded");
  	  }
  	  else {
  		  Log.d(TAG, "B: Contact set verification failed");
	    	  Log.d(TAG, "received verification tag: " + verification.toString());
	    	  Log.d(TAG, "computed verification tag: " + toVerify.toString());
  	  }
  	  
  	  //B sends its aut to A:
  	  
  	  mTarget.get().mMessageService.write(mTarget.get().authObj.getAuth());
  	  
  	  //prepare for receiving computed tags later on:
  	  mTarget.get().T = new ArrayList<BigInteger>();
  	  mTarget.get().T2 = new ArrayList<BigInteger>();
  	  
  	  mTarget.get().state = 9;
    }
    
    else if (mTarget.get().state == 8) {
//  	  //A received B's auth:
//  	  mTarget.get().peerAuth = new BigInteger(readBuf);
//  	  
//  	  //A checks B's auth:
//  	  BigInteger verification = mTarget.get().peerAuth.modPow(mTarget.get().e, mTarget.get().N);
//  	  BigInteger toVerify = BigInteger.ONE;
//  	  for (BigInteger toverifyi : mTarget.get().peerSet) {
//  		  toVerify = toVerify.multiply(toverifyi).mod(mTarget.get().N);
//  	  }
//  	  
//  	  toVerify = mTarget.get().hash(toVerify.toByteArray(), (byte)0).mod(mTarget.get().N);
//  	  if (verification.equals(toVerify)) {
//  		  Log.d(TAG, "A: Contact set verification succeeded");
//  	  }
//  	  else {
//  		  Log.d(TAG, "A: Contact set verification failed");
//	    	  Log.d(TAG, "received verification tag: " + verification.toString());
//	    	  Log.d(TAG, "computed verification tag: " + toVerify.toString());
//  	  }
//  	  
//  	  
//  	  //A performs computations on yis and sends them to B:
//  	  mTarget.get().T = new ArrayList<BigInteger>();
//  	  for (BigInteger t1i : mTarget.get().peerSet) {
//  		  t1i = t1i.modPow(mTarget.get().Ra, mTarget.get().N);
//  		  mTarget.get().T.add(t1i);
//  		  
//  		  mTarget.get().mMessageService.write(t1i);
//  	  }
//  	  
//  	  //prepare for receiving B's computed tags
//  	  mTarget.get().T2 = new ArrayList<BigInteger>();
//  	  
//  	  mTarget.get().state = 10;
    }
    
    else if (mTarget.get().state == 9) {
  	  //B receives A's computed tags:
  	  mTarget.get().T.add(new BigInteger(readBuf));
  	  
  	  
  	  
  	  if (mTarget.get().yis.size() == mTarget.get().T.size()) {
  		  //B performs computations on zis and sends them to A:
  		  for (BigInteger t2i : mTarget.get().peerSet) {
  			  t2i = t2i.modPow(mTarget.get().Rb, mTarget.get().N);
  			  mTarget.get().T2.add(t2i);
  			  
  			  
  			  mTarget.get().mMessageService.write(t2i);
  		  }
  		  
  		  
  		  //output common contacts:
  		  int i0 = 0;
  		  List<String> inCommon2 = new ArrayList<String>();
  		  for (int l = 0; l < mTarget.get().T.size(); l++) {
  			  for (int l2 = 0; l2 < mTarget.get().T2.size(); l2++) {
  				  if (mTarget.get().T.get(l).equals(mTarget.get().T2.get(l2))) {
  					  inCommon2.add(mTarget.get().contactList.get(l2).getFirstName() + " " + mTarget.get().contactList.get(l2).getLastName());
  				  }
  			  }
  		  }
  		  Log.d(TAG, "Found " + inCommon2.size() + " common friends");
  		  
  		  String[] inCommon = new String[inCommon2.size()];
  		  for (int l = 0; l < inCommon2.size(); l++) {
  			  inCommon[l] = inCommon2.get(l);
  		  }
  		  mTarget.get().currentPeer.setCommonFriends(inCommon);
  		  
  		  //now show the result of the PSI to the user in a dialog:
  	      CommonFriendsDialogFragment newFragment = new CommonFriendsDialogFragment(); //DialogFragment
  	      newFragment.setCurrentPeer(mTarget.get().currentPeer);
  	      newFragment.show(mTarget.get().getFragmentManager(), "check-common-friends");
	  
  	      //the user's response to the "become friends" question is handled in the methods below
		  
		  
  		  mTarget.get().state = 1;
  	  }
  	  
    }
    
    else if (mTarget.get().state == 10) {
//  	  //A receives B's computed tags:
//  	  mTarget.get().T2.add(new BigInteger(readBuf));
//  	  
//  	  
//  	  
//  	  if (mTarget.get().zis.size() == mTarget.get().T2.size()) {
//  		  //output common contacts:
//  		  int i0 = 0;
//  		  List<String> inCommon2 = new ArrayList<String>();
//  		  for (int l = 0; l < mTarget.get().T2.size(); l++) {
//  			  for (int l2 = 0; l2 < mTarget.get().T.size(); l2++) {
//  				  if (mTarget.get().T.get(l).equals(mTarget.get().T2.get(l2))) {
//  				  inCommon2.add(mTarget.get().contactList.get(l2).getFirstName() + " " + mTarget.get().contactList.get(l2).getLastName());
//  			  
//  				  }
//  			  }
//  		  }
//  		  Log.d(TAG, "Found " + inCommon2.size() + " common friends");
//  		  
//  		  String[] inCommon = new String[inCommon2.size()];
//  		  for (int l = 0; l < inCommon2.size(); l++) {
//  			  inCommon[l] = inCommon2.get(l);
//  		  }
//  		  mTarget.get().currentPeer.setCommonFriends(inCommon);
//  		  
//  		  //now show the result of the PSI to the user in a dialog:
//  	      CommonFriendsDialogFragment newFragment = new CommonFriendsDialogFragment(); //DialogFragment
//  	      newFragment.setCurrentPeer(mTarget.get().currentPeer);
//  	      newFragment.show(mTarget.get().getFragmentManager(), "check-common-friends");
//	  
//  	      //the user's response to the "become friends" question is handled in the methods below
//  		  
//  		  mTarget.get().state = 1; 
//  	  }
    } */
	
}
