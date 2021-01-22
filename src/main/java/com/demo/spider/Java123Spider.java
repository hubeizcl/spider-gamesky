package com.demo.spider;

import com.alibaba.fastjson.JSON;
import com.demo.http.HttpClientTool;
import com.demo.utils.DownLoadUtils;
import com.demo.utils.HttpClientUtils;
import com.demo.utils.MyTools;
import javafx.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.compile;


public class Java123Spider {
    private static final String CommAddress = "http://www.java1234.com";
    private static final Map<String, String> subFix = JSON.parseObject("{\n" +
            "   \"网页基础技术相关\" : \"list_69_\",\n" +
            "   \"区块链\" : \"list_147_\",\n" +
            "   \"Java新书推荐\" : \"list_161_\",\n" +
            "   \"Java基础相关\" : \"list_65_\",\n" +
            "   \"安卓技术相关\" : \"list_68_\",\n" +
            "   \"大数据云计算\" : \"list_115_\",\n" +
            "   \"数据库技术相关\" : \"list_66_\",\n" +
            "   \"JavaWeb技术相关\" : \"list_67_\"\n" +
            "}\n", Map.class);

    public static void getModuleFirstPageUrl(String startPageUrl) {
        try {
            Document document = Jsoup.connect(startPageUrl).get();
            Elements w960 = document.body().getElementsByClass("w960 center clear mt1");
            Elements pright = w960.get(0).getElementsByClass("pright");
            Elements tbox = pright.get(0).getElementsByClass("tbox");
            Elements d6 = tbox.get(0).getElementsByClass("d6");
            Map<String, String> map = new HashMap<>();
            List<Element> collect = d6.get(0).childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList());
            for (int i = 0; i < collect.size(); i++) {
                Element element = collect.get(i);
                String href = element.childNode(0).attributes().get("href");
                String title = element.childNode(0).childNodes().get(0).toString();
                map.put(title, CommAddress + href);
            }
            System.out.println("查询主题完成");
            System.out.println(JSON.toJSONString(map));
            baseModelFirstPageGetCurrentPageUrl(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void baseModelFirstPageGetCurrentPageUrl(Map<String, String> map) {
        try {
            Map<String, List<String>> moduleUrls = new HashMap<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String moduleMainPag = value + subFix.get(key) + "1.html";
                Document document = Jsoup.connect(moduleMainPag).get();
                Elements w960 = document.body().getElementsByClass("w960 center clear mt1");
                Elements pleft = w960.get(0).getElementsByClass("pleft");
                Elements dede_pages = pleft.get(0).getElementsByClass("dede_pages");
                Elements pagelist = dede_pages.get(0).getElementsByClass("pagelist");
                List<Element> collect = pagelist.get(0).childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList());
                String pageNum = collect.get(collect.size() - 1).childNodes().get(0).childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList()).get(0).childNodes().get(0).toString();
                int i = Integer.parseInt(pageNum);
                MyTools.MyCollection<String> myCollection = MyTools.MyCollection.<String>builder();
                for (int j = 1; j <= i; j++) myCollection.of(value + subFix.get(key) + j + ".html");
                moduleUrls.put(key, myCollection.build2List());
            }
            System.out.println(JSON.toJSONString(moduleUrls));
            basePageGetCurrentAllUrl(moduleUrls);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void basePageGetCurrentAllUrl(Map<String, List<String>> map) {
        try {
            int num = 0;
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String key = entry.getKey();
                List<String> pages = entry.getValue();
                Map<String, List<BookUrl>> allUrls = new HashMap<>();
                List<BookUrl> bookUrls = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++) {
                    String page = pages.get(i);
                    Document document = Jsoup.connect(page).get();
                    Elements w960 = document.body().getElementsByClass("w960 center clear mt1");
                    Elements pleft = w960.get(0).getElementsByClass("pleft");
                    Elements listbox = pleft.get(0).getElementsByClass("listbox");
                    Elements e2 = listbox.get(0).getElementsByClass("e2");
                    List<Element> collect = e2.get(0).childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList());
                    for (Element element : collect) {
                        Element element1 = element.childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList()).get(2);
                        String url = element1.attr("href");
                        String bookTitle = element1.childNodes().get(0).toString();
                        BookUrl bookUrl = new BookUrl();
                        bookUrl.setBookName(bookTitle);
                        bookUrl.setBookUrl(CommAddress + url);
                        bookUrls.add(bookUrl);
                    }
                    System.out.println("获取第" + num + "页链接完成");
                    num++;
                    if (i % 10 == 0) {
                        allUrls.put(key, bookUrls);
                        baseBookUrlGetBaiDuUrlAndPassword(allUrls);
                        bookUrls.clear();
                    }
                }
                allUrls.put(key, bookUrls);
                baseBookUrlGetBaiDuUrlAndPassword(allUrls);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void baseBookUrlGetBaiDuUrlAndPassword(Map<String, List<BookUrl>> allUrls) {
        try {
            for (Map.Entry<String, List<BookUrl>> entry : allUrls.entrySet()) {
                List<BookUrl> value = entry.getValue();
                if (MyTools.MyCollection.isNotEmpty(value))
                    for (BookUrl bookUrl : value) {
                        String url = bookUrl.getBookUrl();
                        Document document = Jsoup.connect(url).get();
                        Elements w960 = document.body().getElementsByClass("w960 center clear mt1");
                        Elements pleft = w960.get(0).getElementsByClass("pleft");
                        Elements viewbox = pleft.get(0).getElementsByClass("viewbox");
                        Elements content = viewbox.get(0).getElementsByClass("content");
                        List<Element> collect = content.get(0).getElementsByTag("table").get(0).getElementsByTag("tbody").get(0).getElementsByTag("td").get(1).childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList());
                        List<Element> collect1 = collect.stream().filter(vo -> vo.text().contains("https://pan.baidu.com")).collect(Collectors.toList());
                        if (MyTools.MyCollection.isNotEmpty(collect1)) {
                            for (Element element : collect1) {
                                List<Element> collect2 = element.childNodes().stream().filter(vo -> vo instanceof Element).map(vo -> (Element) vo).collect(Collectors.toList());
                                for (Element element1 : collect2) {
                                    Elements elements = element1.getElementsByTag("strong");
                                    for (Element element2 : elements) {
                                        if (element2.text().contains("链接")) {
                                            Optional<Node> optional = element2.childNodes().stream().filter(vo -> vo instanceof Element).findFirst();
                                            if (optional.isPresent()) {
                                                Element node = (Element) optional.get();
                                                String attr = node.attr("href");
                                                bookUrl.setBaiDuUrl(attr);
                                            }
                                        }
                                        if (element2.text().contains("提取码")) {
                                            Optional<Node> optional = element2.childNodes().stream().filter(vo -> vo instanceof Element).findFirst();
                                            if (optional.isPresent()) {
                                                Element node = (Element) optional.get();
                                                String text = ((TextNode) node.childNodes().get(0)).text();
                                                bookUrl.setBaiDuPassword(text.substring(0, 4));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            System.out.println(JSON.toJSONString(allUrls));
            writeCsv(allUrls, "C:\\workspace\\spider-gamesky\\src\\main\\resources\\");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void writeCsv(Map<String, List<BookUrl>> allUrls, String path) {
        for (Map.Entry<String, List<BookUrl>> entry : allUrls.entrySet()) {
            String key = entry.getKey();
            List<BookUrl> value = entry.getValue();
            List<String[]> list = new ArrayList<>();
            for (int i = 0; i < value.size(); i++) {
                BookUrl bookUrl = value.get(i);
                list.add(new String[]{bookUrl.getBookName(), bookUrl.getBookUrl(), bookUrl.getBaiDuUrl(), bookUrl.getBaiDuPassword()});
                if (i % 10 == 0) {
                    MyTools.MyCSV.writeFileAppend(path + key + ".csv", list);
                    list.clear();
                }
            }
            MyTools.MyCSV.writeFileAppend(path + key + ".csv", list);
            list.clear();
        }
    }

    public static void main(String[] args) throws IOException {
        getModuleFirstPageUrl("http://www.java1234.com/a/javabook/javabase/");
    }


    public static class BookUrl {
        private String bookName;
        private String bookUrl;
        private String baiDuUrl;
        private String baiDuPassword;

        public String getBookName() {
            return bookName;
        }

        public void setBookName(String bookName) {
            this.bookName = bookName;
        }

        public String getBookUrl() {
            return bookUrl;
        }

        public void setBookUrl(String bookUrl) {
            this.bookUrl = bookUrl;
        }

        public String getBaiDuUrl() {
            return baiDuUrl;
        }

        public void setBaiDuUrl(String baiDuUrl) {
            this.baiDuUrl = baiDuUrl;
        }

        public String getBaiDuPassword() {
            return baiDuPassword;
        }

        public void setBaiDuPassword(String baiDuPassword) {
            this.baiDuPassword = baiDuPassword;
        }
    }
}
