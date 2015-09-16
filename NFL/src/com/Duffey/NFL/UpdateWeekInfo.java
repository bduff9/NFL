package com.Duffey.NFL;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

import com.mrc.dbo.MrcConnection;

import java.sql.*;
import java.net.URL;
import java.io.InputStream;
import java.util.*;

public class UpdateWeekInfo{

	private static final String source = "mysql_nfl";

	public static void main (String argv []) {

		Logger log = Logger.getLogger(UpdateWeekInfo.class);

		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -2);
		int year = c.get(Calendar.YEAR);
		List<String> sql = new ArrayList<String>();
		Boolean weekOver = false;

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			URL XML = new URL("http://football.myfantasyleague.com/" + year + "/export?TYPE=nflSchedule");
			InputStream stream = XML.openStream();
			Document doc = docBuilder.parse(stream);
			doc.getDocumentElement().normalize();
			int week = Integer.parseInt(doc.getDocumentElement().getAttribute("week"));
			NodeList listOfGames = doc.getElementsByTagName("matchup");
			int totalGames = listOfGames.getLength();
			log.info("Week " + week + " - " + totalGames + " Games");
			for (int g = 0; g < totalGames; g++) {
				Node gameNode = listOfGames.item(g);
				if (gameNode.getNodeType() == Node.ELEMENT_NODE) {
					Element game = (Element)gameNode;
					int remaining = Integer.parseInt(game.getAttribute("gameSecondsRemaining"));
					String status;
					if (remaining >= 3600) {
						status = "P";
					} else if (remaining < 3600 && remaining > 2700) {
						status = "1";
					} else if (remaining <= 2700 && remaining > 1800) {
						status = "2";
					} else if (remaining == 1800) {
						status = "H";
					} else if (remaining < 1800 && remaining > 900) {
						status = "3";
					} else if (remaining <= 900 && remaining > 0) {
						status = "4";
					} else if (remaining == 0) {
						status = "C";
					} else { // remaining is less than 0
						status ="I";
					}
					NodeList teams = game.getElementsByTagName("team");
					String homeTeam = "",
							visitingTeam = "",
							homePoss = "",
							visitingPoss = "",
							homeRedZone = "",
							visitingRedZone = "";
					int homeScore = 0,
							visitingScore = 0;
					for (int t=0; t < teams.getLength(); t++) {
						Node teamNode = teams.item(t);
						if(teamNode.getNodeType() == Node.ELEMENT_NODE) {
							Element team = (Element)teamNode;
							if (team.getAttribute("isHome").equals("1")) {
								homeTeam = team.getAttribute("id");
								homeScore = Integer.parseInt("0" + team.getAttribute("score"));
								homePoss = team.getAttribute("hasPossession");
								homeRedZone = team.getAttribute("inRedZone");
							} else {
								visitingTeam = team.getAttribute("id");
								visitingScore = Integer.parseInt("0" + team.getAttribute("score"));
								visitingPoss = team.getAttribute("hasPossession");
								visitingRedZone = team.getAttribute("inRedZone");
							}
						}
					}
					String winner = "",
							hasPoss = "null",
							inRedZone = "null";
					if (homeScore > visitingScore) {
						winner = "HTEAM";
					} else if (homeScore < visitingScore) {
						winner = "VTEAM";
					} else {
						winner = "'TIE'";
					}
					if (homePoss.equals("1")) {
						hasPoss = "'H'";
					} else if (visitingPoss.equals("1")) {
						hasPoss = "'V'";
					}
					if (homeRedZone.equals("1")) {
						inRedZone = "'H'";
					} else if (visitingRedZone.equals("1")) {
						inRedZone = "'V'";
					}
					if (status.equals("C")) {
						sql.add("UPDATE NFL.GAMES SET WINNER = " + winner + ", "
								+ "STATUS = '" + status + "', "
								+ "HSCORE = " + homeScore + ", "
								+ "VSCORE = " + visitingScore + ", "
								+ "TIMELEFT = " + remaining + ", "
								+ "HASPOSS = " + hasPoss + ", "
								+ "REDZONE = " + inRedZone + " "
								+ "WHERE WEEK = " + week + " AND HTEAM='" + homeTeam + "' AND VTEAM='" + visitingTeam + "'");
						if (g == (totalGames-1)) {
							int lastScore = homeScore + visitingScore;
							sql.add("UPDATE NFL.WEEKINFO SET LASTSCORE = " + lastScore + " WHERE WEEK = " + week);
							weekOver = true;
						}
					} else if (!status.equals("P")){
						if (g == 0) {
							sql.add("UPDATE NFL.GAMES SET STATUS = 'C' WHERE WEEK = " + week + " AND HTEAM='BON' AND VTEAM='BON'");
						}
						sql.add("UPDATE NFL.GAMES SET STATUS = '" + status + "', "
								+ "HSCORE = " + homeScore + ", "
								+ "VSCORE = " + visitingScore + ", "
								+ "TIMELEFT = " + remaining + ", "
								+ "HASPOSS = " + hasPoss + ", "
								+ "REDZONE = " + inRedZone + " "
								+ "WHERE WEEK = " + week + " AND HTEAM='" + homeTeam + "' AND VTEAM='" + visitingTeam + "'");
					}
				}
			}
			// Connect to db for updates
			Connection conn = null;
			String usql = "";
			try {
				//Get connection configured in m-power/mrcjava/web-inf/classes/spring-config.xml
				conn = MrcConnection.getConnection(source);
				for (int s = 0; s < sql.size(); s++) {
					/* if (s == 0) {
						Statement stmt = conn.createStatement();
						usql = "UPDATE NFL.TIEBREAKER SET DUMMY = 0 WHERE WEEK = " + week;
						int updated = stmt.executeUpdate(usql);
						log.info(usql + " updated " + updated + " rows");
						stmt.close();
					} */
					Statement stmt = conn.createStatement();
					usql = (String) sql.get(s);
					int updated = stmt.executeUpdate(usql);
					log.info(usql + " updated " + updated + " rows");
					//log.info(usql);
					stmt.close();
				}
				if (!weekOver) {
					week--;
				}
				Statement stmt = conn.createStatement();
				// Reset survivor picks for dead players
				usql = "update NFL.SURVIVOR A"
						+ " join (select B.WEEK as WEEK, B.USERID as USERID from NFL.SURVIVOR B join NFL.GAMES C on B.GAMEID = C.GAMEID where C.WINNER is not null and B.PICK <> C.WINNER) D on A.USERID = D.USERID and A.WEEK > D.WEEK"
						+ " set A.GAMEID = null, A.PICK = null";
				int updated = stmt.executeUpdate(usql);
				log.info(usql + " updated " + updated + " rows");
				for (int wk = week; wk > 0; wk--) {
					usql = "SELECT A.USERID, SUM(A.POINTS), SUM(1), max(D.LASTSCORE) - max(C.SCORE) FROM NFL.PICKS A INNER JOIN NFL.GAMES B ON A.PICK = B.WINNER AND A.GAMEID = B.GAMEID"
							+ " LEFT OUTER JOIN NFL.TIEBREAKER C ON A.USERID = C.USERID AND B.WEEK = C.WEEK LEFT OUTER JOIN NFL.WEEKINFO D ON B.WEEK = D.WEEK"
							+ " WHERE B.WEEK = " + wk + " GROUP BY A.USERID ORDER BY SUM(A.POINTS) DESC, SUM(1) DESC";
					ResultSet res = stmt.executeQuery(usql);
					List<Player> ps = new ArrayList<Player>();
					int highScore = 0;
					int mostCorrect = 0;
					int bestTiebreaker = 999;
					while (res.next()) {
						if (res.getInt(2) >= highScore) {
							Player p = new Player();
							p.setName(res.getString(1));
							p.setPoints(res.getInt(2));
							if (res.isFirst()) highScore = res.getInt(2);
							p.setCorrect(res.getInt(3));
							p.setTiebreaker(res.getInt(4));
							ps.add(p);
							if (res.getInt(3) > mostCorrect) mostCorrect = res.getInt(3);
							if (res.getInt(4) < bestTiebreaker && res.getInt(4) > -1) bestTiebreaker = res.getInt(4);
						}
					}
					if (ps.size() > 1) {
						for (int x = ps.size() -1; x >= 0; x--) {
							if (ps.get(x).getCorrect() != mostCorrect) ps.remove(x);
						}
						if (ps.size() > 1) {
							for (int x = ps.size() - 1; x >= 0; x--) {
								if (ps.get(x).getTiebreaker() != bestTiebreaker) ps.remove(x);
							}
						}
					}
					for (int x = 0; x < ps.size(); x++) {
						usql = "UPDATE NFL.TIEBREAKER SET DUMMY = '999' WHERE USERID = '" + ps.get(x).getName() + "' AND WEEK = " + wk;
						updated = stmt.executeUpdate(usql);
						log.info(usql + " updated " + updated + " rows");
					}
				}
				stmt.close();
			} catch (SQLException sqle) {
				log.error("sql = " + sql, sqle);
			} finally {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (SAXParseException err) {
			log.error("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			log.error(" " + err.getMessage());
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static class Player {
		String name;
		int points;
		int correct;
		int tiebreaker;

		public Player() {
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getPoints() {
			return points;
		}
		public void setPoints(int points) {
			this.points = points;
		}
		public int getCorrect() {
			return correct;
		}
		public void setCorrect(int correct) {
			this.correct = correct;
		}
		public int getTiebreaker() {
			return tiebreaker;
		}
		public void setTiebreaker(int tiebreaker) {
			this.tiebreaker = tiebreaker;
		}
	} 
}