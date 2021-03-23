package com.day.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class GetHundsunVedio {
	private static String url = "http://hundsun.eceibs20.com/index.php?r=front/plan&type=0";
	
	private static void doWork(String url) {

		
		 String result="";
		try {
			result = GetResult.getResult(url);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	        Document document = Jsoup.parse(result);
	        document.setBaseUri(url);//指定base URI
	}
	
	
	public static void main(String[] args) {
		doWork(url);
	}
	
}
