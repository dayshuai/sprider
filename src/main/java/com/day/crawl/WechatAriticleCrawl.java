package com.day.crawl;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.cn.scitc.util.SendEmailUtils;

public class WechatAriticleCrawl {
	public static String USER_AGENT = "User-Agent";
	public static String USER_AGENT_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36";
	public static String COOKIE = "Cookie";
	public static String COOKIEVALUE = "SNUID=1B3758CF1015B78E16EE604D11E5D003; IPLOC=CN3100; SUID=0A2648DE2D18960A000000005EDEFD00; ld=Ikllllllll2WTs1llllllVEW@yllllllT6kujkllll9llllljylll5@@@@@@@@@@; SUV=1591672065673834";
	static Map<String, String> COOKIES = new HashMap<String, String>();
	public static String URL = "https://weixin.sogou.com/weixin?type=1&s_from=input&query=${chatNo}&ie=utf8&_sug_=y&_sug_type_=&w=01019900&sut=9970&sst0=1591664819492&lkt=0%2C0%2C0";
	public static String BASEURL = "https://weixin.sogou.com";
	public static String HOMEURL = "http://www.sogou.com/web?ie=utf8&query=jzfx01";

	static {
		COOKIES.put("JSESSIONID", "aaacDxhBqNxIX9PnO9wix");
	}
	public static String tempTitle = "";

	public static void main(String[] args) {
		int i = 1;
		do {
			Long millis = 60000L;
			LocalTime time = LocalTime.now();
			LocalTime threePMTime = LocalTime.parse("15:00");
			LocalTime nineAmTime = LocalTime.parse("09:00");
			System.out.println("now：" + time + " 第" + i + "次开始...");
			hasUpdated("jzfx01");
			i++;
			try {
				//9:00-15:00 10min刷一次
				if (time.isAfter(threePMTime) || time.isBefore(nineAmTime)) {
					millis = 600000L;
				}
				System.out.println("等待--------------------------------------millis:" + millis + "ms");
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (true);
	}

	public static String getGUID() {
		StringBuilder uid = new StringBuilder();
		// 产生16位的强随机数
		Random rd = new SecureRandom();
		for (int i = 0; i < 16; i++) {
			// 产生0-2的3位随机数
			int type = rd.nextInt(3);
			switch (type) {
			case 0:
				// 0-9的随机数
				uid.append(rd.nextInt(10));
				/*
				 * int random = ThreadLocalRandom.current().ints(0, 10)
				 * .distinct().limit(1).findFirst().getAsInt();
				 */
				break;
			case 1:
				// ASCII在65-90之间为大写,获取大写随机
				uid.append((char) (rd.nextInt(25) + 65));
				break;
			case 2:
				// ASCII在97-122之间为小写，获取小写随机
				uid.append((char) (rd.nextInt(25) + 97));
				break;
			default:
				break;
			}
		}
		return uid.toString();
	}

	private static void hasUpdated(String wechatNo) {
		try {
			//先进入搜狗
			Connection baseCon = Jsoup.connect(HOMEURL); // 获取connection
			baseCon.header(USER_AGENT, USER_AGENT_VALUE); // 配置模拟浏览器
			Response baseRs = baseCon.execute(); // 获取响应
			// Document doBase = Jsoup.parse(baseRs.body());
			//获取cookie
			String cookies = baseRs.cookies().toString().replace("{", "").replace("}", "").replace(",", ";");
			// 生成16位的随机数
			String time = String.valueOf(System.currentTimeMillis()) + getRandomString(3) + ";";
			cookies += ";SUV=" + time;
			URL = URL.replace("${chatNo}", wechatNo);
			//进入公众号页面
			Connection con = Jsoup.connect(URL).header(USER_AGENT, USER_AGENT_VALUE).header(COOKIE, cookies); // 获取connection
			Response rs = con.execute();
			Document dodetail = Jsoup.parse(rs.body());
			Elements AElement = dodetail.select("dd > a");
			String topOne = AElement.text();
			System.out.println("标题：" + topOne);
			String articleUrl = BASEURL + AElement.attr("href");
			System.out.println("文章url：" + articleUrl);
			// 与缓存不同时发邮件
			if (StringUtils.isNotBlank(topOne) && !tempTitle.equals(topOne)) {
				try {
					SendEmailUtils.sendTextEmail(SendEmailUtils.to, wechatNo, articleUrl, topOne);
					tempTitle = topOne;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			/*
			 * Connection con2 = Jsoup.connect(articleUrl); Document doc =
			 * con2.header(USER_AGENT, USER_AGENT_VALUE).cookies(rs.cookies()).get();
			 * System.out.println(doc); doc.
			 */
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getRandomString(int length) {
	    String str = "0123456789";
	    Random random = new Random();
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < length; i++) {
	        int number = random.nextInt(str.length());
	        sb.append(str.charAt(number));
	    }
	    return sb.toString();
	}

}
