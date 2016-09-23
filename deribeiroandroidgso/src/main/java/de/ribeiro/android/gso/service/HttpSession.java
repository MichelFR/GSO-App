package de.ribeiro.android.gso.service;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.dataclasses.Const;

public class HttpSession {
    private static Logger _logger;
    private String _cookie;
    private HttpClient _client;
    private boolean _isInitialized;

    public HttpSession(String url) throws IOException {
        _logger = new Logger(Const.APPFOLDER, "HttpSession");
        Initialize(url);
    }

    private static String[] ConvertToStupidDateArray(GregorianCalendar gc) {
        ArrayList<String> al = new ArrayList<String>();
        al.add(String.valueOf(gc.get(Calendar.YEAR)));
        al.add(PadLeft(String.valueOf(gc.get(Calendar.MONTH) + 1), 2, '0'));
        al.add(PadLeft(String.valueOf(gc.get(Calendar.DAY_OF_MONTH)), 2, '0'));

        String[] result = new String[al.size()];
        int i = 0;
        for (String el : al) {
            result[i] = el;
            i++;
        }

        return result;
    }

    private static String PadLeft(String value, int length, char filler) {
        String result = value;

        for (int i = result.length(); i < length; i++) {
            result = filler + result;
        }
        return result;
    }

    private void Initialize(String url) throws IOException {
        _client = new DefaultHttpClient();
        HttpGet getCookies = new HttpGet(url);
        HttpResponse cookieresponse = _client.execute(getCookies);
        Header[] cookies = cookieresponse.getHeaders("Set-Cookie");
        if (cookies.length >= 2) {
            String value1 = cookies[0].getValue().substring(0, cookies[0].getValue().indexOf(";"));
            String value2 = cookies[1].getValue().substring(0, cookies[1].getValue().indexOf(";"));
            _cookie = value1 + ";" + value2;
            _logger.Trace("Setting up cookie: " + _cookie);
        }
        _isInitialized = true;
    }

    public String PostRequest(String url, HttpPost post) throws IOException {
        if (!_isInitialized)
            Initialize(url);

        post.setHeader("Cookie", _cookie);
        HttpResponse response = _client.execute(post);

        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            return out.toString();
        } else {
            // Closes the connection.
            response.getEntity().getContent().close();
            return null;
        }
    }

    public void SetStupidServerDate(GregorianCalendar gc, int id) throws Exception {
        _logger.Trace("Increasing Serverdate try: " + id);
        if (!_isInitialized)
            throw new Exception("Not Initialized!");

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("id", id);
            jsonObj.put("method", "jsonService.setDate");

            JSONArray params = new JSONArray();
            String[] date = ConvertToStupidDateArray(gc);
            params.put(Integer.valueOf(date[0]));
            params.put(Integer.valueOf(date[1]) - 1);
            params.put(Integer.valueOf(date[2]));
            jsonObj.put("params", params);
        } catch (JSONException e1) {
            _logger.Error("Cannot create JSON Object", e1);
        }

        try {
            HttpPost post = new HttpPost("https://webuntis.stadt-koeln.de/WebUntis/JSON-RPC");
            StringEntity se = new StringEntity(jsonObj.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

            post.setHeader("Content-type", "application/json");
            post.setHeader("Cookie", _cookie);
            post.setEntity(se);
            HttpResponse response = _client.execute(post);

            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                out.toString();
            } else {
                // Closes the connection.
                response.getEntity().getContent().close();
            }

        } catch (Exception e) {
            _logger.Error("Cannot post JSON Request via Http", e);
        }

    }

    public void Close() {
        _cookie = null;
        _client = null;
        _isInitialized = false;
    }
}
