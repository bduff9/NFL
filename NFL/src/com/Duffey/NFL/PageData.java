package com.Duffey.NFL;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.mrc.http.Constants;
import com.mrc.http.Util;
import com.mrc.http.Visitor;
import com.mrc.http.security.SHAHash;
import com.mrc.model.MobileConfig;
import com.mrc.util.CheckEmbedded;
import com.mrc.util.ClientInfo;
import com.mrc.util.PageMaker;
import com.mrc.util.PageMakerUtil;

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

	/*********************************************************************
	 * Make page string
	 *********************************************************************/
	public String makePage(HttpServletRequest req,HttpServletResponse res, PageData page, Visitor visitor, String lib, String skeleton, ServletContext context) {
		String patstr = "<!--\\s*insert\\s* user_file=\"\\S+.\\w+\"\\s*here\\s*-->";
		Pattern pat = Pattern.compile(patstr); 

		Map<String, Object> pageDataMap = new HashMap<String, Object>();
		pageDataMap.put("visitor", visitor);
		pageDataMap.put("page", page);
		pageDataMap.put("contextPath", CheckEmbedded.contextPath);
		PageMakerUtil.setMapData(req, res, pageDataMap, context);

		PageMaker pageMaker = new PageMaker();
		StringWriter writer = new StringWriter();		

		//2011-02-21:add client info for alter skeleton
		ClientInfo client = new ClientInfo();
		HttpSession ses = req.getSession();
		MobileConfig mobileConf0 = (MobileConfig) ses.getAttribute(Constants.SES_KEY_MOBILE_CONF);
		client.setInfo(req, mobileConf0);
		String realpath = visitor.getContextRealPath();

		pageMaker.appPage(pageDataMap, lib, skeleton, realpath, writer);
		String str = writer.toString();

		String mpowerRoot = CheckEmbedded.mrcJavaFolder;

		Matcher matcher = pat.matcher(str);
		String tplsrc = PageMakerUtil.insertLinkFile(str, mpowerRoot, lib, null, matcher, true);

		return tplsrc;
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