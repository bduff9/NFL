package com.Duffey.NFL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ParsePickHTML {

	public static void main(String[] pVal) {

		ArrayList<String[]> allPicks = new ArrayList<String[]>();
		Document doc = null;
		try {
			doc = Jsoup.connect("http://www.4nflpicks.com/poolpicks.html").get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements pickSpans = doc.select("td>span");
		int pickSpanCount = pickSpans.size();
		int picks = 0;
		int week = 0;
		for (int p = 0; p < pickSpanCount; p++) {
			String pick = pickSpans.get(p).text();
			//System.out.println(pick);
			week = (pick.matches("\\.Wk.(\\d{1,2}).") ? Integer.parseInt(pick.replaceAll("\\.Wk.(\\d{1,2}).", "$1")) : week);
			if (pick.matches("\\.\\d{1,2}[^A-Za-z0-9]{1,2}-[^A-Za-z]{1,3}\\S{2,3}.*")) {
				String team = pick.replaceAll("\\.\\d{1,2}[^A-Za-z0-9]{1,2}-[^A-Za-z]{1,3}(\\S{2,3}).*", "$1").trim();
				int points = Integer.parseInt(pick.replaceAll("\\.(\\d{1,2})[^A-Za-z0-9]{1,2}-[^A-Za-z]{1,3}\\S{2,3}.*", "$1"));
				//System.out.println(points + " - " + team);
				// Search list for team
				int inList = -1;
				for (int x = 0; x < allPicks.size(); x++){
					if (allPicks.get(x)[0].equalsIgnoreCase(team)) {
						inList = x;
					}
				}
				// If found, add points and pick number
				if (inList >= 0) {
					int oldPoints = Integer.parseInt(allPicks.get(inList)[1]);
					int oldNum = Integer.parseInt(allPicks.get(inList)[2]);
					oldPoints = oldPoints + points;
					oldNum++;
					allPicks.get(inList)[1] = "" + oldPoints;
					allPicks.get(inList)[2] = "" + oldNum;
				}
				// Otherwise add it
				else {
					String[] newTeam = {team, "" + points, "1"};
					allPicks.add(newTeam);
				}
				picks++;
			}
		}
		Collections.sort(allPicks, new Comparator<String[]> () {
			@Override
			public int compare(String[] a, String[] b) {
				double picksA = Integer.parseInt(a[2]),
						picksB = Integer.parseInt(b[2]),
						pointsA = Integer.parseInt(a[1]),
						pointsB = Integer.parseInt(b[1]),
						avgA = pointsA / picksA,
						avgB = pointsB / picksB;
				
				if (picksA < picksB) {
					return 1;
				} else if (picksA > picksB) {
					return -1;
				} else if (avgA < avgB) {
					return 1;
				} else if (avgA > avgB) {
					return -1;
				} else if (pointsA < pointsB) {
					return 1;
				} else if (pointsA > pointsB) {
					return -1;
				}
				return 0;
			}
		});
		
		int games = picks / 4;
		System.out.println(picks + " total picks in week " + week + " for " + games + " total games");
		for (String[] teamStat : allPicks) {
			String team = teamStat[0];
			double points = Double.parseDouble(teamStat[1]),
					people = Double.parseDouble(teamStat[2]),
					avg = points / people;
			System.out.println(games-- + ". " + team + " - " + (int) points + " points by " + (int) people + " people for an average of " + avg + " points per person");
			if (games == -1) {
				break;
			}
		}
	}

}