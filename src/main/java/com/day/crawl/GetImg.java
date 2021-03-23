package com.day.crawl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
 
public class GetImg {
    public GetImg(String url) throws Exception{
        //获取工具类GetResult返回的html,并用Jsoup解析
        String result = GetResult.getResult(url);
        Document document = Jsoup.parse(result);
        //若HTML文档包含相对URLs路径，需要将这些相对路径转换成绝对路径的URLs
        document.setBaseUri(url);//指定base URI
 
        //获取所有的img元素
        Elements elements = document.select("img");
        int i=1;
        for (Element e : elements) {
            //获取每个src的绝对路径
            String src = e.absUrl("src");
            URL urlSource = new URL(src);
            URLConnection urlConnection = urlSource.openConnection();
 
            //设置图片名字
            String imageName = src.substring(src.lastIndexOf("/") + 1,i++);
 
            //控制台输出图片的src
            System.out.println(e.absUrl("src"));
 
            //通过URLConnection得到一个流，将图片写到流中，并且新建文件保存
            InputStream in = urlConnection.getInputStream();
            OutputStream out = new FileOutputStream(new File("E:\\IDEA\\imgs\\", imageName));
            byte[] buf = new byte[1024];
            int l = 0;
            while ((l = in.read(buf)) != -1) {
                out.write(buf, 0, l);
            }
        }
    }
}