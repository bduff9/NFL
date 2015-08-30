package com.Duffey.NFL;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

import com.mrc.dbo.MrcConnection;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.io.InputStream;
import java.util.*;

public class InitPool{

	// Update the following when ready
	private static final int seasonYear = 2016;
	public static final String source = "mysql_nfl_dev";
	//private static final String source = "mysql_nfl_prod";

	public static void main (String argv []){
		List<String> sql = new ArrayList<String>();
		sql.add("DELETE FROM NFL.GAMES");
		sql.add("DELETE FROM NFL.NFLLOG");
		sql.add("DELETE FROM NFL.PICKS");
		sql.add("DELETE FROM NFL.PLAYERS");
		sql.add("DELETE FROM NFL.TIEBREAKER");
		sql.add("DELETE FROM NFL.WEEKINFO");
		sql.add("DELETE FROM NFL.AUDIT");
		sql.add("DELETE FROM NFL.SURVIVOR");
		for(int week = 1; week < 18; week++){
			try {
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				int year = Calendar.getInstance().get(Calendar.YEAR);
				URL url = new URL("http://football.myfantasyleague.com/" + year + "/export?TYPE=nflSchedule&W=" + week);
				InputStream stream = url.openStream();
				Document doc = docBuilder.parse(stream);
				doc.getDocumentElement().normalize();
				NodeList games = doc.getElementsByTagName("matchup");
				System.out.println ("Week " + week + ": " + games.getLength() + " games");
				String vTeam = "", hTeam = "", kickTimestamp = "";
				for(int game = 1; game <= games.getLength(); game++) {
					Node gameNode = games.item(game-1);
					if(gameNode.getNodeType() == Node.ELEMENT_NODE){
						Element gameElement = (Element)gameNode;
						long kickoff = Long.parseLong(gameElement.getAttribute("kickoff"));
						java.util.Date d = new java.util.Date(kickoff*1000);
						SimpleDateFormat kD = new SimpleDateFormat("yyyy-MM-dd");
						kD.setTimeZone(TimeZone.getTimeZone("CST"));
						SimpleDateFormat kT = new SimpleDateFormat("HH:mm:ss");
						kT.setTimeZone(TimeZone.getTimeZone("CST"));
						String kickDate = kD.format(d);
						String kickTime = kT.format(d);
						kickTimestamp = kD.format(d) + " " + kT.format(d);
						if (game == 1) {							
							sql.add("INSERT INTO NFL.WEEKINFO (`WEEK`, `LASTSCORE`, `FIRSTGAME`, `FIRSTKICK`) VALUES (" + week + ", 0, '" + kickDate + "', '" + kickTime + "')");
							sql.add("INSERT INTO NFL.GAMES (`GAMEID`, `WEEK`, `GAME`, `HTEAM`, `VTEAM`, `WINNER`, `STATUS`, `KICKOFF`) VALUES (" + (week*100) + ", " + week + ", 0, 'BON', 'BON', null, 'P', '" + kickTimestamp + "')");
						}

						NodeList teams = gameElement.getElementsByTagName("team");
						for(int t = 0; t < teams.getLength(); t++){
							Node teamNode = teams.item(t);
							if(teamNode.getNodeType() == Node.ELEMENT_NODE){
								Element teamElement = (Element)teamNode;
								if(teamElement.getAttribute("isHome").equals("0")){
									vTeam = teamElement.getAttribute("id");
								} else {
									hTeam = teamElement.getAttribute("id");
								}
							}
						}
					}
					// Convert team names to same used in ss.xml UPDATE:2013-06-23 - no longer using ss.xml
					//hTeam = convertShortName(hTeam);
					//vTeam = convertShortName(vTeam);
					sql.add("INSERT INTO NFL.GAMES (`GAMEID`, `WEEK`, `GAME`, `HTEAM`, `VTEAM`, `WINNER`, `STATUS`, `KICKOFF`) VALUES (" + (week*100+game) + ", " + week + ", " + game + ", '" + 
							hTeam + "', '" + vTeam + "', null, 'P', '" + kickTimestamp + "')");
				}
			}catch (SAXParseException err) {
				System.out.println ("** Parsing error" + ", line " 
						+ err.getLineNumber () + ", uri " + err.getSystemId ());
				System.out.println(" " + err.getMessage ());
			}catch (SAXException e) {
				Exception x = e.getException ();
				((x == null) ? e : x).printStackTrace ();
			}catch (Throwable t) {
				t.printStackTrace ();
			}//System.exit (0);
		}//done looping through all 17 weeks

		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -7);
		int year = c.get(Calendar.YEAR);
		System.out.println();
		if (year != seasonYear) {
			System.out.println("Error! Season is set to " + seasonYear + " but current year is " + year + ".  Printing SQL statements instead:");
			printSQL(sql);
			throw null;
		}

		/*Initialize pool tables (use getNFLXML's code to open connection)
		 * 1. Clear NFL.GAMES
		 * 2. Clear NFL.NFLLOG
		 * 3. Clear NFL.PICKS
		 * 4. Clear NFL.PLAYERS
		 * 5. Clear NFL.TIEBREAKER
		 * 6. Clear NFL.WEEKINFO
		 * 7. Clear NFL.AUDIT
		 * 8. Finally, update NFL.WEEKINFO and NFL.GAMES
		 */
		Connection conn = null;
		String usql = "";
		try {
			//Get connection configured in m-power/mrcjava/web-inf/classes/spring-config.xml
			conn = MrcConnection.getConnection(source);
			for (int s = 0; s < sql.size(); s++) {
				Statement stmt = conn.createStatement();
				usql = (String) sql.get(s);
				int updated = stmt.executeUpdate(usql);
				System.out.println(usql + " updated " + updated + " rows");
				stmt.close();
				if (s == 6) {
					UpdateTeamInfo.main(null);
				}
			}
		} catch (SQLException sqle) {
			System.out.println("sql = " + usql + " caused error: " + sqle);
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println();
		System.out.println("Successfully finished.  Be sure to update seasonYear on line 21.");

	}//end of main

	private static void printSQL(List<String> sql) {
		for (String sqlStr : sql) {
			System.out.println(sqlStr);
		}
	}

	public static String convertShortName(String shortName){
		if(shortName.equals("NEP")){
			return "NE";
		} else if(shortName.equals("GBP")){
			return "GB";
		} else if(shortName.equals("TBB")){
			return "TB";
		} else if(shortName.equals("NOS")){
			return "NO";
		} else if(shortName.equals("KCC")){
			return "KC";
		} else if(shortName.equals("SDC")){
			return "SD";
		} else if(shortName.equals("SFO")){
			return "SF";
		} else {
			return shortName;
		}
	}
}