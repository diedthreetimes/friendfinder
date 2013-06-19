package com.sprout.friendfinder;

/*
 * Ron: Todo: refactor the protocol from FriendFinder Activity to this class
 */

import java.util.List;

import com.sprout.finderlib.AbstractPSIProtocol;
import com.sprout.finderlib.BluetoothService;

public class ATWPSI extends AbstractPSIProtocol {

	@Override
	protected String conductClientTest(BluetoothService s, List<String> input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String conductServerTest(BluetoothService s, List<String> input) {
		// TODO Auto-generated method stub
		return null;
	}

}
