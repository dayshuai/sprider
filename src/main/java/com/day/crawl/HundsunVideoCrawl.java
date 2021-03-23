package com.day.crawl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HundsunVideoCrawl {
	public static void main(String[] args) {
		getStudyVideo();
//		httpDownload("https://ty.138vcd.com/yunmp4/8150028292989938.mp4","E:\\\\video\\8.mp4");

	}

	public static String LOGIN_URL = "https://github.com/login";
	public static String USER_AGENT = "User-Agent";
	public static String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0";
	static Map<String, String> cookies = new HashMap<String, String>();
	public static String url = "http://hundsun.eceibs20.com/index.php?r=front/plan&type=0";
	public static String baseUrl = "http://hundsun.eceibs20.com";
	static{
		cookies.put("PHPSESSID", "072ijrrt3aavc2i1hq9cj690d6");
	}
	
	private static void getStudyVideo() {
		Document doc = null;
		try {
			// 选课中心页
			doc = Jsoup.connect(url).cookies(cookies).timeout(30000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 课程列表
		Elements elmts = doc.select("div.boxli");
		for (Element element : elmts) {
			element = element.select("a.boxlia").first();
			String showUrl = baseUrl + element.attr("href");
			System.out.println("showUrl:" + showUrl);
			try {
				// 课程进入
				Document learnDo = Jsoup.connect(showUrl).cookies(cookies).timeout(30000).get();
				Elements showElements = learnDo.select("div.courbutton");
				// 进入学习的位置
				for (Element showElement : showElements) {
					Element aherfDom = showElement.select("a").first();
					String startLearnUrl = baseUrl + aherfDom.attr("href");
					System.out.println("startLearn:" + startLearnUrl);
					// 学习视频页进入
					Document vedioDoc = Jsoup.connect(startLearnUrl).cookies(cookies).timeout(30000).get();
					Elements dds = vedioDoc.select("#catalog > dd");
					// 如果一节课里有多个视频
					if (dds != null && dds.size() > 0) {
						for (Element dd : dds) {
							Element elementA = dd.select("span.s2 > a").first();
							String fileName = elementA.text();
							String videoPage = baseUrl + elementA.attr("href");
							//进入视频页并下载
							connectPageAndDownLoadVideo(videoPage, fileName);
						}
					} 
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void connectPageAndDownLoadVideo(String videoPage, String fileName) {
		try {
			String filepath = "E:\\video\\" + fileName + ".mp4";
			File file = new File(filepath);
			if (file.exists()) {
				System.out.println("filepath:已存在--" + filepath);
				// 如果存在了就不再下载
				return;
			}
			Document vedioDoc = Jsoup.connect(videoPage).cookies(cookies).timeout(30000).get();
			String bodyContent = vedioDoc.toString();
			int index = bodyContent.indexOf(".mp4");
			if (index > 0) {
				int beginIndex = bodyContent.indexOf("m4v: ");
				String videoUrl = bodyContent.substring(beginIndex + 6, index + 4);
				System.out.println("videoUrl:" + videoUrl);
				System.out.println("download:----------" + filepath);
				httpDownload(videoUrl, filepath);
				System.out.println("download:----------finish");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static boolean httpDownload(String httpUrl, String saveFile) {
		// 1.下载网络文件
		int byteRead;
		URL url;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return false;
		}

		try {
			// 2.获取链接
			URLConnection conn = url.openConnection();
			// conn.setRequestProperty("referer",
			// "https://www.138vcd.com/static/player/dplayer.html");
			// 3.输入流
			InputStream inStream = conn.getInputStream();
			// 3.写入文件
			FileOutputStream fs = new FileOutputStream(saveFile);

			byte[] buffer = new byte[1024];
			while ((byteRead = inStream.read(buffer)) != -1) {
				fs.write(buffer, 0, byteRead);
			}
			inStream.close();
			fs.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}


	/*
	 * public static void main(String[] args) throws Exception {
	 * 
	 * simulateLogin("tanwenfang", "511t821z805t428w"); // 模拟登陆github的用户名和密码
	 * 
	 * }
	 */

	/**
	 * @param userName 用户名
	 * @param pwd      密码
	 * @throws Exception
	 */
	public static void simulateLogin(String userName, String pwd) throws Exception {

		/*
		 * 第一次请求 grab login form page first 获取登陆提交的表单信息，及修改其提交data数据（login，password）
		 */
		// get the response, which we will post to the action URL(rs.cookies())
		Connection con = Jsoup.connect(LOGIN_URL); // 获取connection
		con.header(USER_AGENT, USER_AGENT_VALUE); // 配置模拟浏览器
		Response rs = con.execute(); // 获取响应
		Document d1 = Jsoup.parse(rs.body()); // 通过Jsoup将返回信息转换为Dom树
		List<Element> eleList = d1.select("form"); // 获取提交form表单，可以通过查看页面源码代码得知

		// 获取cooking和表单属性
		// lets make data map containing all the parameters and its values found in the
		// form
		Map<String, String> datas = new HashMap<>();
		for (Element e : eleList.get(1).getAllElements()) {
			// 设置用户名
			if (e.attr("name").equals("login")) {
				e.attr("value", userName);
			}
			// 设置用户密码
			if (e.attr("name").equals("password")) {
				e.attr("value", pwd);
			}
			// 排除空值表单属性
			if (e.attr("name").length() > 0) {
				datas.put(e.attr("name"), e.attr("value"));
			}
		}

		/*
		 * 第二次请求，以post方式提交表单数据以及cookie信息
		 */
		Connection con2 = Jsoup.connect("https://github.com/session");
		con2.header(USER_AGENT, USER_AGENT_VALUE);
		// 设置cookie和post上面的map数据
		Response login = con2.ignoreContentType(true).followRedirects(true).method(Method.POST).data(datas)
				.cookies(rs.cookies()).execute();
		// 打印，登陆成功后的信息
		System.out.println(login.body());

		// 登陆成功后的cookie信息，可以保存到本地，以后登陆时，只需一次登陆即可
		Map<String, String> map = login.cookies();
		for (String s : map.keySet()) {
			System.out.println(s + " : " + map.get(s));
		}
	}

}
