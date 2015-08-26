package com.Duffey.NFL;

import java.io.File;

public class NFLConst {

	// Session Attributes
	public static final String SESSION_CHECK = "mrc-nfl-login";
	public static final String SESSION_USER = "mrc-nfl-user";
	public static final String SESSION_LIB = "mrc-nfl-lib";
	public static final String SESSION_DS = "mrc-nfl-ds";

	// Security
	public static final String ALGO = "AES";
	public static final byte[] SECRET_KEY = new byte[] { 'T', 'h', 'i', 's', 'I', 's', 'T', 'h', 'e', 'N', 'F', 'L', 'P', 'o', 'o', 'l' };

	// Signon Page
	public static final String LOGIN_SKELETON = "mrcSignon2.html";
	public static final String REMEMBER_ME = "rememberMe";
	public static final int REMEMBER_EXPIRES_DAYS = 150;
	public static final int REMEMBER_EXPIRES_HOURS = 24;
	public static final int REMEMBER_EXPIRES_MINUTES = 60;
	public static final int REMEMBER_EXPIRES_SECONDS = 60;
	public static final int REMEMBER_EXPIRES = REMEMBER_EXPIRES_DAYS * REMEMBER_EXPIRES_HOURS * REMEMBER_EXPIRES_MINUTES * REMEMBER_EXPIRES_SECONDS; // In seconds, value = # days * 24 hours * 60 minutes * 60 seconds
	public static final String FORM_USER = "mrcuser";
	public static final String FORM_PASSWORD = "mrcpswd";

	// Miscellaneous
	public static final String SEP = File.separator;
	public static final String DD = "NFL";
	public static final String MRC_TOKEN = "mrc-nfl-token";

}