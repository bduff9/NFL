package com.Duffey.NFL;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mrc.dbo.MrcConnection;
import com.mrc.http.Constants;
import com.mrc.http.Util;
import com.mrc.http.Visitor;
import com.mrc.http.security.LoginConfig;
import com.mrc.util.Utils;

import mrc.CustomLoginInterface;
import mrc.LoginFilter;

/**
 * @author Duffey
 * Custom login for Graftech
 */
public class Login extends HttpServlet implements CustomLoginInterface {

	private static final long serialVersionUID = 54950495594155150L;
	private Logger log = Logger.getLogger(this.getClass());

	/***********************************************************************
	 * Check user/password
	 ***********************************************************************/
	public void check(HttpServletRequest req, HttpServletResponse res, String lib, ServletContext context)
			throws ServletException, IOException {

		PageData page = new PageData();	

		String action = req.getParameter("action");
		if (action != null) {
			signoff(req, res, action, page, context);
			return; // Signoff done, return
		}

		HttpSession ses = req.getSession();
		Visitor visitor = (Visitor) ses.getAttribute(lib + Constants.SES_KEY_VISITOR);

		/** login from stand alone */
		if (visitor == null ) { // Run login standalone		 
			visitor = new Visitor(req); 
		}  

		page.setFormData(req);

		log.info("sessionId=" + ses.getId());

		//If no cookie (fix WAS) or cookie different (Fix TC), force a redirect. 
		String check = (String) ses.getAttribute(NFLConst.SESSION_CHECK);
		String ssidInCookie = Util.cookieValue(req, "JSESSIONID");
		if (check == null && (ssidInCookie == null || !ssidInCookie.equals(ses.getId()))) {
			ses.setAttribute(NFLConst.SESSION_CHECK, "Y");
			String uri = req.getRequestURI();
			uri = StringUtils.replace(uri, "mrc.", lib + ".");
			res.sendRedirect(uri);
			return;
		}

		String appCredentials = Util.cookieValue(req, NFLConst.MRC_TOKEN);
		if (appCredentials != null) {
			try {
				String rememberMeStr = Encryption.decrypt(appCredentials);
				Gson gson = new Gson();
				Type stringStringMap = new TypeToken<HashMap<String, String>>(){}.getType();
				HashMap<String, String> cred = gson.fromJson(rememberMeStr, stringStringMap);
				if (page.getMrcuser() == null) page.setMrcuser(cred.get(NFLConst.FORM_USER));
				if (page.getMrcpswd() == null) page.setMrcpswd(cred.get(NFLConst.FORM_PASSWORD));
			} catch (Exception e) {
				log.error("Failed to load mrctoken.  mrctoken=" + appCredentials + ", error=" + e);
			}
		} else {
			log.info("No saved credentials found in cookie " + NFLConst.MRC_TOKEN);
		}

		if (page.getMrcuser() != null && page.getMrcpswd() != null) {

			String msg = null;
			LoginConfig config = visitor.getLoginConfig();
			msg = validateUser(config, page);

			ses.setAttribute(NFLConst.SESSION_USER, page.getMrcuser());
			ses.setAttribute(NFLConst.SESSION_LIB, lib);
			ses.setAttribute(NFLConst.SESSION_DS, config.datasource);

			if (msg != null) {
				page.reset(req);	
				page.setMessage(msg);
				String tplStr = page.makePage(req, res, page, visitor, lib, NFLConst.LOGIN_SKELETON, context);
				sendPage(tplStr, lib, res, req);
				return; 
			}			

			registerUser(res, req, lib, page);

			/** Now goto request page*/
			String url = visitor.getRequestUrl();

			//2012-10-09:save signonPassPage to be used in filter
			ses.setAttribute(lib + LoginFilter.MRCSIGN2_SIGNURL_KEY, url);
			res.sendRedirect(url);  
			return;			

		} else { // First time accessing sign on

			String urllib = req.getParameter("lib"); // If run directly from mrc.Login, lib parm will be in url
			ses.setAttribute("urllib", urllib);

			page.init(req);
			log.info("sessionId=" + ses.getId()  + ",  visitor=" + visitor);

		}

		String tplStr = page.makePage(req, res, page, visitor, lib, NFLConst.LOGIN_SKELETON, context);
		sendPage(tplStr, lib, res, req);
	}

	public String checkForRESTful(HttpServletRequest req, HttpServletResponse res, String lib, ServletContext context)
			throws ServletException, IOException {
		String msg = null;
		
		PageData page = new PageData();	
		page.setFormData(req);
		
		HttpSession ses = req.getSession();
		Visitor visitor = (Visitor) ses.getAttribute(lib + Constants.SES_KEY_VISITOR);
		if (visitor == null ) {
			visitor = new Visitor(req); 
		}  
		
		LoginConfig config = visitor.getLoginConfig();
		msg = validateUser(config, page);
		
		return msg;
	}

	private String validateUser(LoginConfig config, PageData page) {
		String ds = config.datasource;
		String qtable = config.validate_table;
		String colUser = config.validate_coluser;
		String colPswd = config.validate_colpswd;

		Connection conn = null;
		try {
			conn = MrcConnection.getConnection(ds);
		} catch (SQLException e) {
			log.error(e);
			e.printStackTrace();
			return e.getMessage();
		}

		String msg = null;

		String userCond = colUser + " = ?";
		//String pswdCond =  colPswd + " = ?";

		String sql = "select " + colUser + ", " + colPswd + " from " 
				+ qtable + " where " + userCond;// + " and " + pswdCond;

		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, page.getMrcuser()); 
			//pstmt.setString( 2, page.getMrcpswd()); // Should always be Y
			ResultSet rs = pstmt.executeQuery();

			if (!rs.next()) {
				msg = NFLConst.USER_NOT_FOUND;
			} else if (!rs.getString(2).equals("Y")) {
				msg = NFLConst.USER_NOT_ACTIVATED;
			}
		} catch (SQLException e) {
			msg = e.getMessage();
			log.error(e);
		}  finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error(e);
			}
		}

		return msg;
	}

	/*********************************************************************
	 * Register user  for login
	 *********************************************************************/
	private void registerUser(HttpServletResponse res, HttpServletRequest req, String lib, PageData page) {
		HttpSession ses = req.getSession();
		Visitor visitor = (Visitor) ses.getAttribute(lib + Constants.SES_KEY_VISITOR);
		visitor.setLoginUser(page.getMrcuser()); // Store user id
		Utils.checkUserPrivileges(visitor, req);

		// Write token
		HashMap<String, String> cred = new HashMap<String, String>();
		cred.put(NFLConst.FORM_USER, page.getMrcuser());
		if (page.isRemember()) {
			cred.put(NFLConst.FORM_PASSWORD, page.getMrcpswd());
		}
		Gson gson = new Gson();
		String mrcToken = gson.toJson(cred);
		try {
			String encryptedMrcToken = Encryption.encrypt(mrcToken);
			Cookie cookie = new Cookie(NFLConst.MRC_TOKEN, encryptedMrcToken);
			cookie.setPath("/mrcjava/");
			cookie.setMaxAge(NFLConst.REMEMBER_EXPIRES);
			res.addCookie(cookie);
		} catch(Exception e) {
			log.error(e);
		}

		return;
	}

	/*********************************************************************
	 * Send page  
	 *********************************************************************/
	private void sendPage(String tplStr, String lib, HttpServletResponse res, HttpServletRequest req) {	
		res.setContentType("text/html; charset=utf-8");
		try {
			PrintWriter out = res.getWriter();
			if (LoginFilter.USE_FILTER && lib != null && lib.trim().length() > 0) {
				tplStr = StringUtils.replace(tplStr, "mrc.Login", lib + ".Login");
			}
			out.println(tplStr);
		} catch (IOException e) {
			log.error(e);
		}
	}

	/***************************************************************************
	 * Signoff
	 **************************************************************************/
	private void signoff(HttpServletRequest req, HttpServletResponse res, String action, PageData page, ServletContext context) {
		HttpSession ses = req.getSession();
		if (ses.isNew()) {
			log.info("Session is new. Skipping signoff...");
			return;
		}

		String lib = (String) ses.getAttribute(Constants.SES_KEY_LIB);
		Visitor visitor = (Visitor) ses.getAttribute(lib + Constants.SES_KEY_VISITOR);
		if (visitor == null || visitor.getLoginUser() == null) {
			log.info("User is null, lib=" + lib + ", skipping signoff...");
			//return;
		}

		log.info(visitor.getLoginUser() + " is logging out, invalidating session...");
		ses.invalidate();

		if (action.equals("1")) { // regular sign off
			String signoffUrl = visitor.getLoginConfig().signoffPage;
			log.info("Successfully signed off, redirecting to :" + signoffUrl);
			if (signoffUrl != null && signoffUrl.trim().length() > 0) {
				try {
					res.sendRedirect(res.encodeRedirectURL(signoffUrl));
				} catch (Exception e) {
					log.error(e);
				}		
			}
		} else { // Switching users
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (int i = 0; i < cookies.length; i++) {
					Cookie cookie = cookies[i];
					String cookieName = cookie.getName().toLowerCase();
					if (cookieName.equals(NFLConst.MRC_TOKEN.toLowerCase())) {
						cookie.setMaxAge(0);
						cookie.setValue(null);
						res.addCookie(cookie);
						break;
					}
				}
			}
			String tplStr = page.makePage(req, res, page, visitor, lib, NFLConst.LOGIN_SKELETON, context);
			sendPage(tplStr, lib, res, req);
		}
	}

}