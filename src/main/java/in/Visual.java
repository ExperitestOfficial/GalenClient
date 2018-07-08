package in;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class Visual {
	private static String accessKey = null;
	private static String lastJsonResult = null;
	private static String lastResultUrl = null;
	private static String lastSummary = null;
	private static boolean debug = false;
	public static boolean verify(WebDriver driver, String pageName) {
		boolean status = false;
		lastJsonResult = null;
		lastResultUrl = null;
		lastSummary = null;
		if(accessKey == null) {
			accessKey = System.getProperty("visual_accessKey");
			if(accessKey == null || accessKey.isEmpty()) {
				accessKey = System.getenv("visual_accessKey");
				if(accessKey == null || accessKey.isEmpty()) {
					throw new RuntimeException("accessKey was not configured. You have a few options to configure accessKey:\n 1. Use Visual.setAccessKey(...).\n 2. Use system propery: 'visual_accessKey'.\n 3. or use an environment variable: 'visual_accessKey'\n");
				}
			}
		}
		try {
			String script = executePost("https://gxy1ugjy4c.execute-api.us-east-1.amazonaws.com/dev", "{\"accessKey\": \"" + accessKey + "\",\"pageName\":\"" + pageName + "\"}").trim();
			if(script.startsWith("\"")) {
				script = script.substring(1);
			}
			if(script.endsWith("\"")) {
				script = script.substring(0, script.length() - 1);
			}
			script = new String( Base64.getDecoder().decode(script), "UTF-8");
			if(debug) {
				System.out.println(script);
			}
			String body = (String)((JavascriptExecutor)driver).executeScript(script);
			if(debug) {
				System.out.println(body);
			}
			String screenShotBase64 = ((TakesScreenshot )driver).getScreenshotAs(OutputType.BASE64);
			body = body.replace("<<SCREEN>>", screenShotBase64);
			lastJsonResult = executePost("https://f3a265wlhl.execute-api.us-east-1.amazonaws.com/dev", body);
			if(debug) {
				System.out.println(lastJsonResult);
			}
			JSONObject json = new JSONObject(lastJsonResult);
			if(json.has("resultUrl")) {
				lastResultUrl = json.getString("resultUrl");
			}
			if(json.has("status")) {
				status = json.getBoolean("status");
			}
			if(json.has("summary")) {
				lastSummary = json.getString("summary");
				System.out.println("Visual verify page: " + pageName);
				System.out.println(lastSummary);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return status;
	}
	
	public static String getLastJsonResult() {
		return lastJsonResult;
	}
	public static void setLastJsonResult(String lastJsonResult) {
		Visual.lastJsonResult = lastJsonResult;
	}
	public static String getLastResultUrl() {
		return lastResultUrl;
	}
	public static void setLastResultUrl(String lastResultUrl) {
		Visual.lastResultUrl = lastResultUrl;
	}
	public static String getLastSummary() {
		return lastSummary;
	}
	public static void setLastSummary(String lastSummary) {
		Visual.lastSummary = lastSummary;
	}
	public static boolean isDebug() {
		return debug;
	}
	public static void setDebug(boolean debug) {
		Visual.debug = debug;
	}
	public static void setAccessKey(String accessKey) {
		Visual.accessKey = accessKey;
	}

	private static String executePost(String urlTxt, String body) throws Exception{
		URL url = new URL(urlTxt);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");

		String input = body;

		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes());
		os.flush();

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
				+ conn.getResponseCode());
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));
		StringBuffer buf = new StringBuffer();
		String output;
		while ((output = br.readLine()) != null) {
			buf.append(output); buf.append("\r\n");
		}
		conn.disconnect();
		return buf.toString();
	}

}
