package com.Duffey.NFL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParsePickHTML {

	public static void main(String[] unused) {
		Document	doc = null;
		Elements	pickSpans = null;
		List<String[]>	allPicks = new ArrayList<String[]>();
		String[]	newTeam;
		Boolean	found;
		Double	pt, ct, avg;
		Integer	picks = 0,
				week = 0,
				gameCt = 0,
				people = 0,
				points, oldPoints, oldNum;
		String	pick, team;

		try {
			doc = Jsoup.connect("http://www.4nflpicks.com/poolpicks.html").get();
		} catch (Exception ex) {
			System.out.println("Failed to get picks: " + ex);

			return;
		}

		pickSpans = doc.select("td>span");
		picks = 0;
		week = 0;

		for (Element pickEl : pickSpans) {
			pick = pickEl.text();
			week = (pick.matches("\\.Wk.(\\d{1,2}).*") ? Integer.parseInt(pick.replaceAll("\\.Wk.(\\d{1,2}).*", "$1")) : week);

			if (pick.matches("\\.\\d{1,2}[^A-Za-z0-9]{1,2}-[^A-Za-z]{1,3}\\S{2,3}.*")) {
				team = pick.replaceAll("\\.\\d{1,2}[^A-Za-z0-9]{1,2}-[^A-Za-z]{1,3}(\\S{2,3}).*", "$1").trim();
				points = Integer.parseInt(pick.replaceAll("\\.(\\d{1,2})[^A-Za-z0-9]{1,2}-[^A-Za-z]{1,3}\\S{2,3}.*", "$1"));

				if (points > gameCt) gameCt = points.intValue();

				found = false;

				// Search list for team
				for (String[] teamPick : allPicks) {

					// If found, add points and pick number
					if (teamPick[0].equalsIgnoreCase(team)) {
						found = true;
						oldPoints = Integer.parseInt(teamPick[1], 10);
						oldNum = Integer.parseInt(teamPick[2], 10);
						oldPoints = oldPoints + points;
						oldNum++;
						teamPick[1] = "" + oldPoints;
						teamPick[2] = "" + oldNum;
					}
				}

				// Otherwise add it
				if (!found) {
					newTeam = new String[]{ team, "" + points, "1" };

					allPicks.add(newTeam);
				}

				picks++;
			}
		}

		Collections.sort(allPicks, new Comparator<String[]> () {

			@Override
			public int compare(String[] a, String[] b) {
				double	picksA = Integer.parseInt(a[2]),
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

		people = picks / gameCt;

		System.out.println(picks + " total picks (by " + people + " people) in week " + week + " for " + gameCt + " total games");

		System.out.println("---------------------------------------------------------------------------------------");

		for (String[] teamStat : allPicks) {
			team = teamStat[0];
			pt = Double.parseDouble(teamStat[1]);
			ct = Double.parseDouble(teamStat[2]);
			avg = pt / ct;

			if (gameCt == 0) System.out.println("---------------------------------------------------------------------------------------");

			System.out.println(String.format("%2d", gameCt--) + ". " + String.format("%3s",team) + " - " + String.format("%2d", pt.intValue()) + " points by " + ct.intValue() + " people for an average of " + String.format("%5.2f", (Math.round(avg * 100.0) / 100.0)) + " points per person");

			if (gameCt == -1) break;
		}
	}
}