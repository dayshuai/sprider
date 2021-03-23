package com.day.crawl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cn.download.core.utils.FileUtil;

public class JiraSpider {

	public static String LOGIN_URL = "http://jira.chinatrc.com.cn/login.jsp";
	public static String USER_AGENT = "User-Agent";
	public static String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0";
	static Map<String, String> cookies = new HashMap<String, String>();
	public static String url = "http://jira.chinatrc.com.cn/browse/";
	public static String baseUrl = "http://jira.chinatrc.com.cn";
	public static String JSESSIONID = "1EC3EEA33CDD8A4E4F9305B09289CCFD";
	public static String JIRAIDS = "TRS-8067";
	public static String JIRACONTENTBASEPATH = "D:\\jira\\";
	static {
		cookies.put("JSESSIONID", JSESSIONID);
	}

	
	/**
	 * @param userName 用户名
	 * @param pwd      密码
	 * @throws Exception
	 */
	public static void simulateLogin(String userName, String pwd) throws Exception {
		/*
		 * 第一次请求 grab login form page first 获取登陆提交的表单信息，及修改其提交data数据（login，password）
		 */
		Connection con = Jsoup.connect(LOGIN_URL); // 获取connection
		con.header(USER_AGENT, USER_AGENT_VALUE); // 配置模拟浏览器
		Response rs = con.execute(); // 获取响应
		//用户名密码
		Map<String, String> datas = new HashMap<>();
		datas.put("os_username", userName);
		datas.put("os_password", pwd);
		datas.put("login", "登录");
		/*
		 * 第二次请求，以post方式提交表单数据以及cookie信息
		 */
		Connection con2 = Jsoup.connect(LOGIN_URL);
		con2.header(USER_AGENT, USER_AGENT_VALUE);
		// 设置cookie和post上面的gmap数据
		Response login = con2.ignoreContentType(true).followRedirects(true).method(Method.POST).data(datas)
				.cookies(rs.cookies()).execute();
		
		// 登陆成功后的cookie信息，可以保存到本地，以后登陆时，只需一次登陆即可
		Map<String, String> map = login.cookies();
		for (String s : map.keySet()) {
			if (s.equals("JSESSIONID")) {
				System.out.println(s + " : " + map.get(s));
				JSESSIONID = map.get(s);
				cookies.put("JSESSIONID", JSESSIONID);
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		simulateLogin("hs_huangsheng", "zxd.123456");
		String[] jiraIdArr = JIRAIDS.split(",");
		for (int i = 0; i < jiraIdArr.length; i++) {
			try {
				System.out.println(String.format("开始抓取-----jira:%s", jiraIdArr[i]));
				getDemandDescAndAttachment(jiraIdArr[i]);
			} catch (Exception e) {
				System.out.println(String.format("jira:%s下载失败。", jiraIdArr[i]));
			}
		}
	}
	
	
	/**
	 * 获取需求描述和附件
	 * @param jiraId
	 */
	private static void getDemandDescAndAttachment(String jiraId) {
		String jiraDirPath = JIRACONTENTBASEPATH + jiraId + "\\";
		// 创建文件夹
		File jiraDir = new File(jiraDirPath);
		if (!jiraDir.exists()) {
			jiraDir.mkdir();
		} else {
			FileUtil.delAllFile(jiraDir);
		}
		Document doc = null;
		try {
			// jira 编号
			doc = Jsoup.connect(url + jiraId).cookies(cookies).timeout(30000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 需求描述
		Elements demandDescriptionEle = doc.select("div.twixi-wrap > div.flooded");
		String demandDescriptionTxts = demandDescriptionEle.text().replaceAll("\\s", "\r\n");
		System.out.println(String.format("生成需求描述：jira:%s", jiraId));
		FileUtil.summaryFile(demandDescriptionTxts, jiraDirPath + jiraId + ".txt");
		Elements attachmnetsEle = doc.select("li.attachment-content > dl > dt > a");
		for (Element attachmentA : attachmnetsEle) {
			String attchmentName = attachmentA.text();
			String attchmentPath = baseUrl + attachmentA.attr("href");
			downloadFile(jiraDirPath, attchmentPath, attchmentName);
		}

	}
	/**
	 * 下载文件
	 * @param jiraDir
	 * @param attchmentPath
	 * @param attchmentName
	 */
	private static void downloadFile(String jiraDir, String attchmentPath, String attchmentName) {
		String filepath = jiraDir + attchmentName;
		File file = new File(filepath);
		if (file.exists()) {
			// 如果存在了就重命名
			filepath = filepath.replace(file.getName(), "new_" + file.getName());
		}
		
		
		
		
		
		
		System.out.println("attchmentUrl:" + attchmentPath);
		System.out.println("download:----------" + attchmentPath);
		httpDownloadWithSession(attchmentPath, filepath, JSESSIONID);
		System.out.println("download:----------finish");

	}
	/**
	 * 通过url下载文件
	 * @param httpUrl
	 * @param saveFile
	 * @param sessionID
	 * @return
	 */
	public static boolean httpDownloadWithSession(String httpUrl, String saveFile, String sessionID) {
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
			System.out.println(String.format("下载文件时sessionid:%s", sessionID));
			conn.setRequestProperty("Cookie", "JSESSIONID=" + sessionID);
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
	
	
}
