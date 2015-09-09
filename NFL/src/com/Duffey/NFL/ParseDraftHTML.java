package com.Duffey.NFL;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParseDraftHTML {

	static ArrayList<String> rankerCols = new ArrayList<String>();
	static ArrayList<String> teams = new ArrayList<String>();
	static Connection conn = null;

	private static Logger log = Logger.getLogger(ParseDraftHTML.class);

	public static void main(String[] parms) {
		runDraft(null, null);
	}

	@SuppressWarnings("resource")
	public static void runDraft(Connection con, String[] parms) {
		int playersRanked;
		if (con != null) {
			conn = con;
			PreparedStatement getCols = null;
			PreparedStatement init = null;
			try {
				getCols = conn.prepareStatement("select * from NFL.NFLPLAYERS;");
				ResultSet cols = getCols.executeQuery();
				int colNum = cols.getMetaData().getColumnCount();
				for (int i = 5; i < colNum; i++) {
					rankerCols.add(cols.getMetaData().getColumnName(i));
				}
			} catch (SQLException e) {
				log.error(e.getMessage());
			} finally {
				if (getCols != null) {
					try {
						getCols.close();
					} catch (SQLException e) {
						log.error(e.getMessage());
					}
				}
			}

			String initString = "update NFL.NFLPLAYERS set ";
			for (int i = 0, rankCols = rankerCols.size(); i < rankCols; i++) {
				initString += ((i > 0) ? ", " : "") + rankerCols.get(i) + " = 0";
			}
			try {
				init = conn.prepareStatement(initString);
				init.executeUpdate();
			} catch (SQLException e) {
				log.error(e.getMessage());
			} finally {
				if (init != null) {
					try {
						init.close();
					} catch (SQLException e) {
						log.error(e.getMessage());
					}
				}
			}
		}
		playersRanked = getCBS("http://fantasynews.cbssports.com/fantasyfootball/rankings/top200/yearly");
		System.out.println("CBS - " + playersRanked + " players ranked");
		playersRanked = getNFL("http://www.nfl.com/fantasyfootball/rankings#tabset=pr-top");
		System.out.println("NFL - " + playersRanked + " players ranked");
		playersRanked = getESPN("http://espn.go.com/fantasy/football/story/_/page/FFLranks14top300");
		System.out.println("ESPN - " + playersRanked + " players ranked");
		playersRanked = getYahoo("http://sports.yahoo.com/news/2014-fantasy-rankings");
		System.out.println("Yahoo - " + playersRanked + " players ranked");
	}

	public static int getCBS(String url) {
		int rankers, rank, players = 0;
		String ranker, fullName, firstName, team, name, position;
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements rankTable = doc.select("table.data");
		Elements theRankers = doc.select("a[rel=author]");
		rankers = rankTable.size();
		for (int r = 0; r < rankers; r++) {
			fullName = theRankers.get(r).text();
			firstName = fullName.substring(0, fullName.indexOf(" "));
			Elements rows = rankTable.get(r).select("tr");
			players = rows.size();
			for (int p = 1; p < players; p++) {
				Elements playerInfo = rows.get(p).select("td");
				rank = Integer.parseInt(playerInfo.get(0).text().replaceAll("\\D+",""));
				ranker = "CBS" + firstName.toUpperCase();
				String[] playerData = playerInfo.get(1).text().split(", ");
				String[] nameAndTeam = playerData[0].split(" ");
				team = nameAndTeam[nameAndTeam.length - 1].trim();
				name = "";
				for (int n = 0; n < nameAndTeam.length - 1; n++) {
					name = name + " " + nameAndTeam[n];
				}
				name = name.trim();
				String[] positionAndBye = playerData[1].split(" \\(Bye ");
				position = positionAndBye[0].replaceAll("[^a-zA-Z]+", "");
				int bye = 0;
				if(positionAndBye.length > 1) {
					bye = Integer.parseInt(positionAndBye[1].replaceAll("\\D+",""));
				}
				try {
					generateSQL(team, position, ranker, rank, name, bye);
				} catch (SQLException e) {
					log.error(e.getMessage());
				}
			}
		}
		return players;
	}

	// Currently will not work due to their JavaScript loading
	public static int getNFL(String url) {
		int positions, rankers, rank, bye, players = 0;
		String ranker, fullName, firstName, team, name, position;
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements positionDivs = doc.select("div[id^=pr-]");
		positions = positionDivs.size();
		for (int pos = 0; pos < positions; pos++) {
			position = positionDivs.get(pos).attr("id").replace("pr-", "").toUpperCase();
			if (position.length() <= 3 && !position.equalsIgnoreCase("top")) {
				Element rankTable = positionDivs.get(pos).select("table.result-target").first();
				Elements theRankers = positionDivs.get(pos).select(".editor > h2");
				rankers = theRankers.size();
				for (int r = 0; r < rankers; r++) {
					int firstCols = Integer.parseInt(positionDivs.get(pos).select(".head-grad1").get(0).attr("colspan"));
					fullName = theRankers.get(r).text();
					firstName = fullName.substring(0, fullName.indexOf(" "));
					Elements rows = rankTable.select("tr");
					players = rows.size();
					System.out.println(rankTable.html());
					for (int p = 1; p < players; p++) {
						Element playerInfo = rows.get(p);
						name = playerInfo.select("a").text().trim();
						rank = Integer.parseInt(playerInfo.select("td").get(firstCols + 1 + r).text().replaceAll("\\D+",""));
						ranker = "NFL" + firstName.toUpperCase();
						team = playerInfo.select("td").get(2).text().trim();
						System.out.println(team);
						bye = Integer.parseInt(playerInfo.select("td").get(3).text().replaceAll("\\D+",""));
						try {
							generateSQL(team, position, ranker, rank, name, bye);
						} catch (SQLException e) {
							log.error(e.getMessage());
						}
					}
				}
			}
		}
		return players;
	}

	public static int getESPN(String url) {
		//TODO - fill in method
		return 0;
	}
	
	public static int getYahoo(String url) {
		//TODO - fill in method
		return 0;
	}

	public static boolean checkRanker(String ranker) throws SQLException {
		if (rankerCols.contains(ranker)) {
			// Already has it
		} else {
			PreparedStatement addRanker = null;
			String addRankerString = "alter table NFL.NFLPLAYERS add column " + ranker + " integer unsigned;";
			if (conn != null) {
				try {
					addRanker = conn.prepareStatement(addRankerString);
					addRanker.executeUpdate();
				} catch (SQLException e) {
					log.error(e.getMessage());
				} finally {
					if (addRanker != null) {
						addRanker.close();
					}
				}
				rankerCols.add(ranker);
			}
		}
		return true;
	}

	public static void generateSQL(String team, String position, String ranker, int rank, String name, int bye) throws SQLException {
		PreparedStatement updatePlayer = null;
		PreparedStatement insertPlayer = null;
		PreparedStatement updateTeam = null;

		String updatePlayerString = "update NFL.NFLPLAYERS set `" + ranker + "` = ? "
				+ "where `POSITION` = ?, `SHORT` = (select `SHORT` from NFL.TEAMS where ALTSHORT = ?) and FULLNAME = ?;";
		String insertPlayerString = "insert into NFL.NFLPLAYERS (`FULLNAME`, `SHORT`, `POSITION`, `" + ranker + "`) values "
				+ "(?, (select `SHORT` from NFL.TEAMS where ALTSHORT = ?), ?, ?);";
		String updateTeamString = "update NFL.TEAMS set `BYEWEEK` = ? where ALTSHORT = ?;";

		if (conn != null) {
			try {
				updatePlayer = conn.prepareStatement(updatePlayerString);
				updatePlayer.setInt(1, rank);
				updatePlayer.setString(2, position);
				updatePlayer.setString(3, team);
				updatePlayer.setString(4, name);
				int found = updatePlayer.executeUpdate();
				if (found == 0) {
					insertPlayer = conn.prepareStatement(insertPlayerString);
					insertPlayer.setString(1, name);
					insertPlayer.setString(2, team);
					insertPlayer.setString(3, position);
					insertPlayer.setInt(4, rank);
					insertPlayer.executeUpdate();
				}
			} catch (SQLException e) {
				log.error(e.getMessage());
			} finally {
				if (updatePlayer != null) {
					updatePlayer.close();
				}
				if (insertPlayer != null) {
					insertPlayer.close();
				}
			}

			try {
				updateTeam = conn.prepareStatement(updateTeamString);
				updateTeam.setInt(1, bye);
				updateTeam.setString(2, team);
				updateTeam.executeUpdate();
			} catch (SQLException e) {
				log.error(e.getMessage());
			} finally {
				if (updateTeam != null) {
					updateTeam.close();
				}
			}
		}
	}
}