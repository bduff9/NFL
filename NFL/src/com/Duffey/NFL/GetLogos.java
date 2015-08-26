package com.Duffey.NFL;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class GetLogos {
	
	public static void main(String argv[]) throws MalformedURLException {
		for (int x = 1; x < 1001; x++) {
			URL url = new URL("http://a.espncdn.com/i/teamlogos/nfl/" + x + "/sd.png");
			try {
				InputStream input = url.openStream();
				System.out.println(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
