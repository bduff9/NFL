package com.Duffey.NFL;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.mrc.dbo.MrcConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.io.InputStream;

public class UpdateTeamInfo {

	public static void main(String argv[]) {

		Logger log = Logger.getLogger(UpdateTeamInfo.class);

		try {
			List<String> uSql = new ArrayList<String>();
			List<String> iSql = new ArrayList<String>();

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			URL url = new URL("http://www.barcodegames.com/Teams.xml");
			InputStream stream = url.openStream();
			Document doc = docBuilder.parse(stream);

			// normalize text representation
			doc.getDocumentElement().normalize();
			log.info("Root element of the doc is " + doc.getDocumentElement().getNodeName());
			NodeList leagues = doc.getElementsByTagName("LeagueFormat");
			for (int l = 0; l < leagues.getLength(); l++) {
				Node leagueNode = leagues.item(l);
				if (leagueNode.getNodeType() == Node.ELEMENT_NODE) {
					Element leagueElement = (Element) leagueNode;
					String league = leagueElement.getAttribute("Format");
					if (league.equals("NFL")) {
						NodeList listOfTeams = leagueElement.getElementsByTagName("Team");
						int totalTeams = listOfTeams.getLength();
						log.info("There are " + totalTeams + " NFL teams");
						NodeList conferences = leagueElement.getElementsByTagName("Conference");
						for (int c = 0; c < conferences.getLength(); c++) {
							Node conferenceNode = conferences.item(c);
							if (conferenceNode.getNodeType() == Node.ELEMENT_NODE) {
								Element conferenceElement = (Element) conferenceNode;
								String conference = conferenceElement.getAttribute("Name");
								NodeList divisions = conferenceElement.getElementsByTagName("Division");
								for (int d = 0; d < divisions.getLength(); d++) {
									Node divisionNode = divisions.item(d);
									if (divisionNode.getNodeType() == Node.ELEMENT_NODE) {
										Element divisionElement = (Element) divisionNode;
										String division = divisionElement.getAttribute("Name");
										NodeList teams = divisionElement.getElementsByTagName("Team");
										for (int t = 0; t < teams.getLength(); t++) {
											Node teamNode = teams.item(t);
											if (teamNode.getNodeType() == Node.ELEMENT_NODE) {
												Element teamElement = (Element) teamNode;
												String city = teamElement.getAttribute("City");
												String mascot = teamElement.getAttribute("Mascot").replace("Bronchos", "Broncos");
												int teamId = Integer.parseInt(teamElement.getAttribute("ID"));
												String shortName = teamElement.getAttribute("Abbreviation").toUpperCase();
												int rank = Integer.parseInt(teamElement.getAttribute("LastYearStandings"));
												String logo = teamElement.getAttribute("Logo");
												String logoSmall = teamElement.getAttribute("LogoSmall");
												String color = teamElement.getAttribute("Color");
												String color2 = teamElement.getAttribute("Color2");
												uSql.add("UPDATE NFL.TEAMS SET CITY='"
														+ city
														+ "', NAME='"
														+ mascot
														+ "', SHORT='"
														+ (shortName + mascot.substring(0, 1)).substring(0, 3).replace('4', 'O').replace('X', 'C')
														+ "', ALTSHORT='"
														+ shortName
														+ "', DIVNAME='"
														+ conference
														+ "', DIVDIR='"
														+ division
														+ "', RANK='"
														+ rank
														+ "', LOGO='"
														+ logo
														+ "', LOGOSM='"
														+ logoSmall
														+ "', COLOR='"
														+ color
														+ "', COLOR2='"
														+ color2
														+ "' WHERE TEAMID = "
														+ teamId);
												iSql.add("INSERT INTO NFL.TEAMS (TEAMID, CITY, NAME, SHORT, ALTSHORT, DIVNAME, DIVDIR, RANK, LOGO, LOGOSM, COLOR, COLOR2) VALUES ("
														+ teamId
														+ ", '"
														+ city
														+ "', '"
														+ mascot
														+ "', '"
														+ (shortName + mascot.substring(0, 1)).substring(0, 3).replace('4', 'O')
														+ "', '"
														+ shortName
														+ "', '"
														+ conference
														+ "', '"
														+ division
														+ "', '"
														+ rank
														+ "', '"
														+ logo
														+ "', '"
														+ logoSmall
														+ "', '"
														+ color
														+ "', '"
														+ color2
														+ "')");
											}
										}
									}
								}
							}
						}
					}
				}
			}
			uSql.add("UPDATE NFL.TEAMS SET CITY = 'Tie', NAME = '', SHORT = 'TIE', ALTSHORT = 'TIE', DIVNAME = 'AFC', DIVDIR = 'North', RANK = 0, LOGO = '', LOGOSM = '', COLOR = '', COLOR2 = '' WHERE TEAMID = 33");
			iSql.add("INSERT INTO NFL.TEAMS (TEAMID, CITY, NAME, SHORT, ALTSHORT, DIVNAME, DIVDIR, RANK, LOGO, LOGOSM, COLOR, COLOR2) VALUES (33, 'Tie', '', 'TIE', 'TIE', 'AFC', 'North', 0, '', '', '', '')");
			uSql.add("UPDATE NFL.TEAMS SET CITY = 'Bonus', NAME = 'Points', SHORT = 'BON', ALTSHORT = 'BON', DIVNAME = 'NFC', DIVDIR = 'West', RANK = 0, LOGO = '', LOGOSM = '', COLOR = '', COLOR2 = '' WHERE TEAMID = 34");
			iSql.add("INSERT INTO NFL.TEAMS (TEAMID, CITY, NAME, SHORT, ALTSHORT, DIVNAME, DIVDIR, RANK, LOGO, LOGOSM, COLOR, COLOR2) VALUES (34, 'Bonus', 'Points', 'BON', 'BON', 'NFC', 'West', 0, '', '', '', '')");
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MONTH, -2);
			int year = c.get(Calendar.YEAR);
			try {
				docBuilderFactory = DocumentBuilderFactory.newInstance();
				docBuilder = docBuilderFactory.newDocumentBuilder();
				URL XML = new URL("http://football.myfantasyleague.com/" + year + "/export?TYPE=nflSchedule");
				stream = XML.openStream();
				doc = docBuilder.parse(stream);
				doc.getDocumentElement().normalize();
				NodeList listOfGames = doc.getElementsByTagName("matchup");
				int totalGames = listOfGames.getLength();
				for (int g = 0; g < totalGames; g++) {
					Node gameNode = listOfGames.item(g);
					if (gameNode.getNodeType() == Node.ELEMENT_NODE) {
						Element game = (Element)gameNode;
						NodeList teams = game.getElementsByTagName("team");
						//String homeTeam = "", visitingTeam = "";
						for (int t = 0; t < teams.getLength(); t++) {
							Node teamNode = teams.item(t);
							if(teamNode.getNodeType() == Node.ELEMENT_NODE) {
								Element team = (Element)teamNode;
								/*if (team.getAttribute("isHome") == "1") {
									homeTeam = team.getAttribute("id");
								} else {
									visitingTeam = team.getAttribute("id");
								}*/
								uSql.add("UPDATE NFL.TEAMS SET RUSHOFFENSE=0" + team.getAttribute("rushOffenseRank") + ", RUSHDEFENSE=0" + team.getAttribute("rushDefenseRank")
										+ ", PASSOFFENSE=0" + team.getAttribute("passOffenseRank") + ", PASSDEFENSE=0" + team.getAttribute("passDefenseRank")
										+ ", HASPOSSESSION='" + (team.getAttribute("hasPossession") == "" ? 0 : team.getAttribute("hasPossession"))
										+ "', INREDZONE='" + (team.getAttribute("inRedZone") == "" ? 0 : team.getAttribute("inRedZone")) + "' WHERE SHORT='" + team.getAttribute("id") + "'");
							}
						}
					}
				}

				// Get connection
				Connection conn = null;
				String sql = "";
				try {
					// Get connection configured in m-power/mrcjava/web-inf/classes/spring-config.xml
					conn = MrcConnection.getConnection("mysql_nfl_prod");
					Statement stmt = conn.createStatement();
					for (int s = 0; s < uSql.size(); s++) {
						sql = uSql.get(s);
						int updated = stmt.executeUpdate(sql);
						log.info(sql + " updated " + updated + " rows");
						if (updated == 0 && s <= iSql.size()) {
							sql = iSql.get(s);
							int inserted = stmt.executeUpdate(sql);
							log.info(sql + " inserted " + inserted + " row");
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
				System.out.println("** Parsing error" + ", line "
						+ err.getLineNumber() + ", uri " + err.getSystemId());
				System.out.println(" " + err.getMessage());
			} catch (SAXException e) {
				Exception x = e.getException();
				((x == null) ? e : x).printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}// System.exit (0);
		} catch (SAXParseException err) {
			log.error("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			log.error(" " + err.getMessage());
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}// end of main
}