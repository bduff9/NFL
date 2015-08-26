package com.Duffey.NFL;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mrc.http.Constants;
import com.mrc.http.Util;
import com.mrc.http.Visitor;
import com.mrc.http.security.IValidation;
import com.mrc.http.security.LoginConfig;
import com.mrc.http.security.ValidatorStore;
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

		String action = req.getParameter("signoff");
		if (action != null && action.equals("1")) {
			signoff(req, res);
			return; // Signoff done, return
		}

		HttpSession ses = req.getSession();
		Visitor visitor = (Visitor) ses.getAttribute(lib + Constants.SES_KEY_VISITOR);

		/** login from stand alone */
		if (visitor == null ) { // Run login standalone		 
			visitor = new Visitor(req); 
		}  

		PageData page = new PageData();	
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
		}

		if (page.getMrcuser() != null && page.getMrcpswd() != null) {

			LoginConfig config = visitor.getLoginConfig();
			String ds = config.datasource;
			String qtable = config.validate_table;
			String colUser = config.validate_coluser;
			String colPswd = config.validate_colpswd;
			String sha = config.validate_encryption_type;
			IValidation validator = ValidatorStore.getTableValidator(ds, qtable, colUser, colPswd, sha);
			String msg = validator.validateUser(page.getMrcuser(), page.getMrcpswd(), config.input_idcase.equals("1"), config.input_pwcase.equals("1"));

			ses.setAttribute(NFLConst.SESSION_USER, page.getMrcuser());
			ses.setAttribute(NFLConst.SESSION_LIB, lib);
			ses.setAttribute(NFLConst.SESSION_DS, config.datasource);

			if (msg != null) {
				page.reset(req);	
				page.setMessage(msg);
				HashMap<String, String> resMap = new HashMap<String, String>();
				resMap.put("error", msg);
				Gson gson = new Gson();
				String jsonStr = gson.toJson(resMap);
				sendJSON(jsonStr, lib, res, req);
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

			String signOnHTML = getSignon(lib, req);
			//signOnHTML = OfflineResources.parseMainSection(signOnHTML).replace("{{mrcuser}}", page.getMrcuser()).replace("{{tries}}", "" + page.getTries());
			HashMap<String, String> resMap = new HashMap<String, String>();
			resMap.put("signOn", signOnHTML);
			Gson gson = new Gson();
			String jsonStr = gson.toJson(resMap);
			sendJSON(jsonStr, lib, res, req);

		}

	}

	private String getSignon(String lib, HttpServletRequest req) {
		String sep = NFLConst.SEP;
		ServletContext cntxt = req.getServletContext();
		String signOnPath = cntxt.getRealPath(sep + "WEB-INF" + sep + "classes" + sep + lib + sep + NFLConst.LOGIN_SKELETON);
		File f = new File(signOnPath);
		try {
			return FileUtils.readFileToString(f);
		} catch (IOException e) {
			log.error(e.getMessage());
			return null;
		}
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
	 * Send JSON Response  
	 *********************************************************************/
	private void sendJSON(String jsonStr, String lib, HttpServletResponse res, HttpServletRequest req) {	
		try {
			res.setContentType("text/json; charset=utf-8");
			res.setStatus(HttpServletResponse.SC_OK);

			PrintWriter writer = res.getWriter();
			writer.print(jsonStr);
			writer.flush();
		} catch (IOException e) {
			log.error(e);
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/***************************************************************************
	 * Signoff.
	 **************************************************************************/
	private void signoff(HttpServletRequest req, HttpServletResponse res) {
		HttpSession ses = req.getSession();
		if (ses.isNew()) {
			log.info("Session is new. Skipping signoff...");
			return;
		}

		String lib = (String) ses.getAttribute(Constants.SES_KEY_LIB);
		Visitor visitor = (Visitor) ses.getAttribute(lib + Constants.SES_KEY_VISITOR);
		if (visitor == null || visitor.getLoginUser() == null) {
			log.info("User is null, lib=" + lib + ", skipping signoff...");
			return;
		}

		log.info(visitor.getLoginUser() + " is logging out, invalidating session...");
		ses.invalidate();

		String signoffUrl = visitor.getLoginConfig().signoffPage;
		log.info("Successfully signed off, redirecting to :" + signoffUrl);
		if (signoffUrl != null && signoffUrl.trim().length() > 0) {
			try {
				res.sendRedirect(res.encodeRedirectURL(signoffUrl));
			} catch (Exception e) {
				log.error(e);
			}		
		}
	}

}