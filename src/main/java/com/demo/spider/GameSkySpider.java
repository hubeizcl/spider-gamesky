package com.demo.spider;

import com.alibaba.fastjson.JSON;
import com.demo.utils.DownLoadUtils;
import com.demo.utils.HttpClientUtils;
import javafx.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;


public class GameSkySpider {

    /**
     * 获取想要下载的page
     * <p>
     * https://db2.gamersky.com/LabelJsonpAjax.aspx?callback=jQuery183007489476026424047_1569080090816&jsondata=
     * {"type":"updatenodelabel","isCache":true,"cacheTime":600,"nodeId":"20117","isNodeId":"true","page":6}&_=
     *
     * @param page
     * @return
     * @throws Exception
     */
    public static String page(String url, int page) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "updatenodelabel");
        map.put("isCache", true);
        map.put("cacheTime", 600);
        map.put("nodeId", "20117");
        map.put("isNodeId", "true");
        map.put("page", page);
        HttpPost httpPost = HttpClientUtils.getHttpPost(url);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("jsondata", JSON.toJSONString(map)));
        nvps.add(new BasicNameValuePair("callback", "jQuery183007489476026424047_1569080090816"));
        nvps.add(new BasicNameValuePair("_", System.currentTimeMillis() + ""));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
        String content = HttpClientUtils.getContent(httpPost);
        return content;
    }

    /**
     * 获取当前页面的每个item的url
     * jQuery183007489476026424047_1569080090816({"status":"ok","totalPages":29,"body":"\r\n                \r\n              \r\n                    \r\n\t\t\t\t\t<li>\r\n\t\t\t\t\t\t
     * <div class=\"img\"><a href=\"https://www.gamersky.com/ent/201803/1028766.shtml\" target=\"_blank\">\n
     * <img src=\"https://imgs.gamersky.com/upimg/2018/201803241937164375.jpg\" alt=\"每周精选壁纸：想人生，美景良辰堪惜\"
     * width=\"200\" height=\"110\" class=\"pe_u_thumb\" border=\"0\">\n          </a></div>\r\n\t\t\t\t\t\t<div class=\"con\">\r\n\t\t\t\t\t\t\t
     * <div class=\"tit\"><a href=\"https://www.gamersky.com/ent/201803/1028766.shtml\" target=\"_blank\" title=\"每周壁纸精选第81弹：想人生，美景良辰
     * 堪惜\">每周精选壁纸：想人生，美景良辰堪惜</a></div>\r\n\t\t\t\t\t\t\t<div class=\"txt\">所有图片皆来自于网络，若有侵权请联系邮箱：foxjr@gamersky.com，
     * 我们会及时删除。友情提示：点击图片即可获得高清无水印大图。</div>\r\n\t\t\t\t\t\t\t<div class=\"tme2\"><div class=\"time\">2018-03-24 49:03</div>
     * <div class=\"pls cy_comment\" data-sid=\"1028766\"></div></div>\r\n\t\t\t\t\t\t</div>\r\n\t\t\t\t\t</li>\r\n\t\t\t\t\t\r\n\t\t\t\t\t<
     * li>\r\n\t\t\t\t\t\t<div class=\"img\">
     * <p>
     * <a href=\"https://www.gamersky.com/ent/201801/1002394.shtml\" target=\"_blank\" title=\"每周壁纸精选第73弹：剑战横空金气肃，旌旗映日彩云飞\">
     *
     * @param context
     * @return
     */
    public static Pair<Integer, List<String>> items(String context) {
        List<String> urls = new ArrayList<>();
        if (StringUtils.isNotBlank(context)) {
            context = context.replace("\\r", "").replace("\\n", "").replace("\\t", "").replace("\\", "");
        }
        Matcher matcher = compile("<a href=.*?target=").matcher(context);
        Matcher matcher1 = compile("\"totalPages\":.*?,").matcher(context);
        Set<String> set = new HashSet<>();
        while (matcher.find()) {
            String url = matcher.group();
            String[] split = url.split("\"");
            if (split[1].endsWith("shtml")) {
                set.add(split[1]);
            }
        }
        int totalPage = 0;
        while (matcher1.find()) {
            String url = matcher1.group();
            String totalPageStr = url.split(":")[1];
            totalPage = compile("\\d{1,}").matcher(totalPageStr).find() ? Integer.parseInt(totalPageStr.replace(",", "")) : totalPage;
        }

        urls.addAll(set);
        Pair<Integer, List<String>> pair = new Pair<>(totalPage, urls);
        return pair;
    }

    /**
     * 获取item的内容
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static String item(String url) throws Exception {
        HttpPost httpPost = HttpClientUtils.getHttpPost(url);
        String context = HttpClientUtils.getContent(httpPost);
        return context;
    }

    /**
     * 获取每个item里面有多少页
     * <p>
     * <!--{pe.begin.pagination}--><span id="pe100_page_contentpage" class="pagecss"><div class="page_css">
     * <b><a href="https://www.gamersky.com/ent/201801/1002394.shtml">1</a></b> <a href="https://www.gamersky.com/ent/201801/1002394_2.shtml">2</a>
     * <a href="https://www.gamersky.com/ent/201801/1002394_3.shtml">3</a> <a href="https://www.gamersky.com/ent/201801/1002394_4.shtml">4</a>
     * <a href="https://www.gamersky.com/ent/201801/1002394_5.shtml">5</a> <a href="https://www.gamersky.com/ent/201801/1002394_6.shtml">6</a>
     *
     * @param content
     * @param url
     * @return
     */
    public static int itemMaxPageSize(String content, String url) {
        String newUrl = url.replace(".shtml", "").replace("https", "http");
        int max1 = getMax(content, newUrl);
        url = url.replace(".shtml", "");
        int max2 = getMax(content, url);
        return max1 == 0 ? max2 : max1;
    }

    private static int getMax(String content, String newUrl) {
        int max = 0;
        Matcher matcher = compile("<a href=\"" + newUrl + "_.*?shtml\">").matcher(content);
        Pattern compile2 = compile("\\d{1,}");
        while (matcher.find()) {
            String group = matcher.group();
            if (StringUtils.isNotBlank(group)) {
                group = group.replace(newUrl, "").replace("_", "").replace(".shtml", "");
                Matcher matcher2 = compile2.matcher(group);
                if (matcher2.find()) {
                    int i = Integer.parseInt(matcher2.group());
                    if (i > max) {
                        max = i;
                    }
                }
            }
        }
        return max;
    }

    /**
     * 获取item每页包含image的url
     *
     * <p align="center"><a href="http://www.gamersky.com/showimage/id_gamersky.shtml?http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_01origin_01_20181131846728.jpg" target="_blank"><img class="picact" alt="游民星空" src="http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_01small_02_20181131846AD1.jpg" border="0"></a></p>
     * <p align="center"><a href="http://www.gamersky.com/showimage/id_gamersky.shtml?http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_02origin_03_20181131846EDF.jpg" target="_blank"><img class="picact" alt="游民星空" src="http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_02small_04_20181131846174.jpg" border="0"></a></p>
     * <p align="center"><a href="http://www.gamersky.com/showimage/id_gamersky.shtml?http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_03origin_05_2018113184649E.jpg" target="_blank"><img class="picact" alt="游民星空" src="http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_03small_06_201811318467AE.jpg" border="0"></a></p>
     * <p align="center"><a href="http://www.gamersky.com/showimage/id_gamersky.shtml?http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_04origin_07_20181131846289.jpg" target="_blank"><img class="picact" alt="游民星空" src="http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_04small_08_201811318464FC.jpg" border="0"></a></p>
     * <p align="center"><a href="http://www.gamersky.com/showimage/id_gamersky.shtml?http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_05origin_09_2018113184669E.jpg" target="_blank"><img class="picact" alt="游民星空" src="http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_05small_10_201811318469E2.jpg" border="0"></a></p>
     * <p align="center"><a href="http://www.gamersky.com/showimage/id_gamersky.shtml?http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_06origin_11_20181131846D26.jpg" target="_blank"><img class="picact" alt="游民星空" src="http://img1.gamersky.com/image2018/01/20180113_zl_91_2/gamersky_06small_12_2018113184616A.jpg" border="0"></a></p>
     *
     * @param context
     * @return
     */
    public static List<String> image(String context) {
        List<String> urls = new ArrayList<>();
        Matcher matcher1 = compile("<a target=\"_blank\" href=\".*?><img class=").matcher(context);
        Matcher matcher2 = compile("<a href=.*? target=\"_blank\"><img class").matcher(context);
        Set<String> set = new HashSet<>();
        while (matcher1.find()) {
            String[] split = matcher1.group().split("\"");
            if (split[3].endsWith(".jpg")) {
                set.add(split[3]);
            }
        }
        if (null == set || set.size() <= 0) {
            while (matcher2.find()) {
                String[] split = matcher2.group().split("\"");
                if (split[1].endsWith(".jpg")) {
                    set.add(split[1]);
                }
            }
        }
        urls.addAll(set);
        return urls;
    }

    public static void spider() throws Exception {
        String path = "E:\\迅雷下载";
        String url = "https://db2.gamersky.com/LabelJsonpAjax.aspx";
        File file = new File(path);
        File[] files = file.listFiles();
        Set<String> set = new HashSet<>();
        if (null != files && files.length > 0) {
            for (File fileImage : files) {
                set.add(fileImage.getAbsoluteFile().getName());
            }
        }
        int imageNum = 1;
        Pair<Integer, List<String>> pair = items(page(url, 1));
        imageNum = downLoad(path, set, imageNum, 1, pair.getValue());
        Integer key = pair.getKey();
        for (int i = 2; i <= key; i++) {
            List<String> itemsUrl = items(page(url, i)).getValue();
            imageNum = downLoad(path, set, imageNum, i, itemsUrl);
        }
    }

    private static int downLoad(String path, Set<String> set, int imageNum, int i, List<String> itemsUrl) throws Exception {
        if (null != itemsUrl && itemsUrl.size() > 0) {
            for (String itemUrl : itemsUrl) {
                if (set.contains(itemUrl)) {
                    continue;
                }
                set.add(itemUrl);
                String item = item(itemUrl);
                int itemMaxPageSize = itemMaxPageSize(item, itemUrl);
                for (int j = 1; j <= itemMaxPageSize; j++) {
                    List<String> imageUrls = image(item);
                    if (null != imageUrls && imageUrls.size() > 0) {
                        for (String imageUrl : imageUrls) {
                            if (set.contains(imageUrl)) {
                                continue;
                            }
                            set.add(imageUrl);
                            DownLoadUtils.download(imageUrl, path, null);
                            System.out.println("下载第" + imageNum + "张");
                            imageNum++;
                        }
                    }
                }
            }
        }
        System.out.println("下载第" + i + "页");
        return imageNum;
    }

    public static void main(String[] args) throws Exception {
        spider();
    }
}
