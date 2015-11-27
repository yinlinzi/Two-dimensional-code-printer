package com.inspiry.demo;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpUtil {
	private static final String url = "http://qidian.epbox.cn/?g=api&m=piaowu&a=scan_interface";

	public static String send(String id, String macCode, String k)
			throws Exception {
		long t = System.currentTimeMillis();
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("id", id));
		urlParameters.add(new BasicNameValuePair("mac_code", macCode));
		urlParameters.add(new BasicNameValuePair("timestamp", "" + t));
		urlParameters.add(new BasicNameValuePair("k", k));
		String md5 = id + macCode + t;
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] md5Bytes = md.digest(md5.getBytes());
		urlParameters.add(new BasicNameValuePair("sign", new String(md5Bytes)));

		System.out.println(id);
		System.out.println(macCode);
		System.out.println(t);
		System.out.println(new String(md5Bytes));

		post.setEntity(new UrlEncodedFormEntity(urlParameters));
		HttpResponse response = client.execute(post);
		String strResponse = EntityUtils.toString(response.getEntity());
		return strResponse;
	}
}
