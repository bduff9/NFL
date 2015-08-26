package com.Duffey.NFL;

import javax.servlet.http.HttpServletRequest;
import com.mrc.http.Util;
import com.mrc.http.security.SHAHash;

public class PageData {

	private String mrcuser = "",
			mrcpswd = "",
			message = "",
			formlib,
			/** hash ssid from user */
			ssidUser,
			/** hash ssid on server */
			ssidServer;
	private int tries;
	private boolean remember;

	/*********************************************************************
	 * Get data from form
	 *********************************************************************/
	public void setFormData(HttpServletRequest req) {

		setMrcuser(Util.getRequestParam(req, NFLConst.FORM_USER, null));
		setMrcpswd(Util.getRequestParam(req, NFLConst.FORM_PASSWORD, null));
		setRemember(Util.getRequestParamBool(req, NFLConst.REMEMBER_ME, "Y"));
		setTries(Util.getRequestParam(req, "tries", 0));

		// SSID
		setSsidUser(Util.getRequestParam(req, "t1", "")); //hash ssid from user
		setSsidServer(ssidHash(req));

	}

	/*********************************************************************
	 * Get hash ssid
	 *********************************************************************/
	public String ssidHash(HttpServletRequest req) {
		String ssid0 = req.getSession().getId();
		SHAHash sha = new SHAHash();
		String sid = sha.hash(ssid0, "SHA-256"); //hash ssid on server
		return sid;
	}

	/*********************************************************************
	 * init set form
	 *********************************************************************/
	public void init(HttpServletRequest req) {
		if (getMrcuser() == null) mrcuser = "";
		if (getMrcpswd() == null) mrcpswd = "";
		ssidUser = ssidHash(req);
	}

	/*********************************************************************
	 * Reset form when there is error
	 *********************************************************************/
	public void reset(HttpServletRequest req) {
		mrcpswd = "";
		ssidUser = ssidHash(req);
		tries++;
	}

	public String getMrcpswd() {
		return mrcpswd;
	}

	public void setMrcpswd(String mrcpswd) {
		this.mrcpswd = mrcpswd;
	}

	public String getMrcuser() {
		return mrcuser;
	}

	public void setMrcuser(String mrcuser) {
		this.mrcuser = mrcuser;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getTries() {
		return tries;
	}

	public void setTries(int tries) {
		this.tries = tries;
	}

	public boolean isRemember() {
		return remember;
	}

	public void setRemember(boolean remember) {
		this.remember = remember;
	}

	public String getFormlib() {
		return formlib;
	}

	public void setFormlib(String formlib) {
		this.formlib = formlib;
	}

	public String getSsidUser() {
		return ssidUser;
	}

	public void setSsidUser(String ssidUser) {
		this.ssidUser = ssidUser;
	}

	public String getSsidServer() {
		return ssidServer;
	}

	public void setSsidServer(String ssidServer) {
		this.ssidServer = ssidServer;
	}

}