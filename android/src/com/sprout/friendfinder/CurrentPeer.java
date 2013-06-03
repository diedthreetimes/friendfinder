/* Ron
 * handle the information of the current peer (i.e. the peer that we are connected to at the moment) here
 */

package com.sprout.friendfinder;

public class CurrentPeer {
	
	private String id;
	private String name;
	private String[] inCommon;
	
	CurrentPeer() {
		
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setCommonFriends(String[] friends) {
		this.inCommon = friends;
	}
	
	public String[] getCommonFriends() {
		//rather return list of names here (can fetch them from own offline friends list based on ids) - not ids..
		return this.inCommon;
	}
}
