package tumject;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.request.RequestBuilder;

public class TumblrLikes {
	private Map<String, Map<String, Set<String>>> attributeTypeMap = new HashMap<String, Map<String, Set<String>>>();
	private Map<String, Integer> attributeCount = new HashMap<String, Integer>();
	private Set<String> allPostTypes = new TreeSet<String>();
	
	public static void maine(String[] args) throws IOException {
		Calendar c = Calendar.getInstance();
		c.set(2016, Calendar.NOVEMBER, 13, 0, 0, 0);

		System.out.println(c.getTime() + ", " + c.getTimeInMillis()/1000 + ", " + c.getTimeInMillis() + ", " + c);
	}

	public static void mainGetLikes(String[] args) throws IOException {
		TumblrLikes tl = new TumblrLikes();
		
		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));
		
		// Authenticate via OAuth
		JumblrClient client = new JumblrClient(
		  prop.getProperty("tumblr.key.1"),
		  prop.getProperty("tumblr.key.2")
		);

		client.setToken(
		  prop.getProperty("tumblr.key.3"),
		  prop.getProperty("tumblr.key.4")
		);

		Map<String, Object> options = new HashMap<String, Object>();

		Calendar c = Calendar.getInstance();
		c.set(2016, Calendar.NOVEMBER, 13, 0, 0, 0);
		String beforeTime = Long.toString(c.getTimeInMillis()/1000);
		options.put("before", beforeTime);

		int maxCalls = 999;
		boolean done = false;
		while (!done) {
			String likesJson = getLikesJson(client, options, prop);
			PrintWriter pw = new PrintWriter(new File(prop.getProperty("tumblr.dir") + "/likes/likes_" + beforeTime + ".txt"));
			pw.println(likesJson);
			pw.flush();
			pw.close();

			JsonReader jr = Json.createReader(new StringReader(likesJson));
			JsonObject jo = jr.readObject();
			pw = new PrintWriter(new File(prop.getProperty("tumblr.dir") + "/likes/likes_" + beforeTime + ".json"));
			pw.println(jo.toString());
			pw.flush();
			pw.close();

			String newBeforeTime = tl.getEarliestLike(jo, true);
			if (newBeforeTime==null || beforeTime.equals(newBeforeTime)) done = true;
			beforeTime = newBeforeTime;
			options.put("before", beforeTime);

			maxCalls--;
			if (maxCalls==0) done = true;
		}

/*
		List<Post> posts = client.userLikes(options);
		for (Post post : posts) {
//			System.out.println(post.toString() + "\t" + post.getTimestamp() + "\t" + post.getDateGMT());

			dumpObject(post);
			System.out.println();
			System.out.println("----");
			System.out.println();
		}
*/
	}

	public String getEarliestLike(JsonObject jo, boolean verbose) {
		long earliestLike = Long.MAX_VALUE;
		JsonArray likedPosts = jo.getJsonObject("response").getJsonArray("liked_posts");
		for (int i=0; i<likedPosts.size(); i++) {
			JsonObject post = likedPosts.getJsonObject(i);
			int likeTime = post.getInt("liked_timestamp");

			if (verbose) System.out.println(post.getString("blog_name") + "\t" + post.getJsonNumber("id").longValue() + "\t" + post.getString("type") + "\t" + post.getString("video_type", "") + "\t" + post.getInt("liked_timestamp"));
			if (likeTime<earliestLike) earliestLike = likeTime;
			
			for (String key : post.keySet()) {
				Integer count = attributeCount.get(key);
				if (count==null) count = 0;
				attributeCount.put(key, 1+count);

				Map<String, Set<String>> typeMap = attributeTypeMap.get(key);
				if (typeMap==null) {
					typeMap = new HashMap<String, Set<String>>();
					attributeTypeMap.put(key, typeMap);
				}
				String valueType = post.get(key).getValueType().toString();
				if (valueType.equals("TRUE") || valueType.equals("FALSE")) valueType = "BOOLEAN";
				String postType = post.getString("type");

				allPostTypes.add(postType);
				Set<String> typeSet = typeMap.get(postType);
				if (typeSet==null) {
					typeSet = new TreeSet<String>();
					typeMap.put(postType, typeSet);
				}
				typeSet.add(valueType);
			}
		}

		if (earliestLike == Long.MAX_VALUE || likedPosts.size() == 0) return null;
		return Long.toString(earliestLike);
	}

	public void listReblogs(JsonObject jo) {
		JsonArray likedPosts = jo.getJsonObject("response").getJsonArray("liked_posts");
		for (int i=0; i<likedPosts.size(); i++) {
			JsonObject post = likedPosts.getJsonObject(i);

			String firstReblog = "";
			try {
				firstReblog = post.getJsonArray("trail").getJsonObject(0).getJsonObject("blog").getString("name");
			} catch (Exception e) {
			}
			System.out.println(post.getString("blog_name") + "\t" + post.getJsonNumber("id").longValue() + "\t" + post.getString("type") + "\t" + firstReblog);
		}
	}

	public void printTypeInfo() {
		for (String postType : allPostTypes) System.out.print("\tcount\t" + postType);
		System.out.println();
		
		for (String attribute : attributeTypeMap.keySet()) {
			System.out.print(attribute + "\t" + attributeCount.get(attribute));
			for (String postType : allPostTypes) {
				Set<String> valueTypeSet = attributeTypeMap.get(attribute).get(postType);
				if (valueTypeSet==null) valueTypeSet = new TreeSet<String>();
				String types = "\t", sep = "";
				for (String valueType : valueTypeSet) {
					types = types + sep + valueType;
					sep = "; ";
				}
				System.out.print(types);
			}
			System.out.println();
		}
	}
	
	private static String getLikesJson(JumblrClient client, Map<String, ?> options, Properties prop) {
//		client.userLikes();
		RequestBuilder rb = new RequestBuilder(client);
//		ResponseWrapper rw = rb.get("/user/likes", options);
        OAuthRequest request = rb.constructGet("/user/likes", options);

        Token token = new Token(prop.getProperty("tumblr.key.3"), prop.getProperty("tumblr.key.4")); // token key, token secret
        OAuthService service= new ServiceBuilder().
                provider(TumblrApi.class).
                apiKey(prop.getProperty("tumblr.key.1")).apiSecret(prop.getProperty("tumblr.key.2")). // api key, api secret
                build();
        service.signRequest(token, request);

        Response response = request.send();
        if (response.getCode() == 200 || response.getCode() == 201) {
            String json = response.getBody();
            System.out.println(json);
            return json;
        } else {
        	System.out.println("Response code: " + response.getCode());
        }
/*		
        Gson gson = gsonParser();
        JsonObject object = (JsonObject) response;
        List<Post> l = gson.fromJson(object.get("liked_posts"), new TypeToken<List<Post>>() {}.getType());
        for (Post e : l) { e.setClient(client); }
        return l;
*/
        return null;
	}
	
	public static void main(String[] args) throws IOException {
		TumblrLikes tl = new TumblrLikes();
//		BufferedReader br = new BufferedReader(new FileReader("c:/Users/martella/Documents/tumblr/likes.sample.json"));
//		String jsonString = br.readLine();
//		System.out.println(jsonString);

		JsonReader jr = null;
		JsonObject jo = null;
		
		File dir = new File("c:/Users/martella/Documents/tumblr/likes/");
		File[] files = dir.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".json")) return true;
				return false;
			}});
		
		for (File file : files) {
			jr = Json.createReader(new FileReader(file));
			jo = jr.readObject();

//			String earliestLike = tl.getEarliestLike(jo, false);
//			System.out.println("Earliest Like: " + earliestLike);

//			tl.findText(jo, "grinch", true);

			tl.listReblogs(jo);
		}

		tl.printTypeInfo();
	}

	private void findText(JsonObject jo, String string, boolean verbose) {
		JsonArray likedPosts = jo.getJsonObject("response").getJsonArray("liked_posts");
		for (int i=0; i<likedPosts.size(); i++) {
			JsonObject post = likedPosts.getJsonObject(i);

//			if (verbose) System.out.println(post.getString("blog_name") + "\t" + post.getJsonNumber("id").longValue() + "\t" + post.getString("type") + "\t" + post.getString("video_type", "") + "\t" + post.getInt("liked_timestamp"));

			try {
				String body = post.getString("body");
				if (body.toLowerCase().contains(string.toLowerCase())) System.out.println(post);
			} catch (Exception e) {
			}
			/*
			for (String key : post.keySet()) {
				Integer count = attributeCount.get(key);
				if (count==null) count = 0;
				attributeCount.put(key, 1+count);

				Map<String, Set<String>> typeMap = attributeTypeMap.get(key);
				if (typeMap==null) {
					typeMap = new HashMap<String, Set<String>>();
					attributeTypeMap.put(key, typeMap);
				}
				String valueType = post.get(key).getValueType().toString();
				if (valueType.equals("TRUE") || valueType.equals("FALSE")) valueType = "BOOLEAN";
				String postType = post.getString("type");

				allPostTypes.add(postType);
				Set<String> typeSet = typeMap.get(postType);
				if (typeSet==null) {
					typeSet = new TreeSet<String>();
					typeMap.put(postType, typeSet);
				}
				typeSet.add(valueType);
			}
			*/
		}

	}

	private static void printJson(JsonObject jo, int depth) {
		StringBuffer buffer = new StringBuffer();
		for (int i=0; i<depth; i++) buffer.append("  ");
		for (String key : jo.keySet()) {
			JsonValue jv = jo.get(key);
			if (JsonValue.ValueType.OBJECT.equals(jv.getValueType())) {
				System.out.println(buffer + "\"" + key + "\": {");
				printJson(jo.getJsonObject(key), depth+1);
				System.out.println(buffer + "}");
			} else if (JsonValue.ValueType.ARRAY.equals(jv.getValueType())) {
				System.out.println(buffer + "\"" + key + "\": [");
				printJson(jo.getJsonArray(key), depth+1);
				System.out.println(buffer + "]");
			} else if (JsonValue.ValueType.STRING.equals(jv.getValueType())) {
				System.out.println(buffer + "\"" + key + "\": \"" + jo.getString(key).replaceAll("\n", " \\n ") + "\"");
			} else {
				System.out.println(buffer + "\"" + key + "\": " + jo.get(key));
			}
//			System.out.println(key + " [" + jv.getValueType() + ", " + jv.getClass() + "]: " + jv);
		}
	}

	private static void printJson(JsonArray ja, int depth) {
		StringBuffer buffer = new StringBuffer();
		for (int i=0; i<depth; i++) buffer.append("  ");
		for (int i=0; i<ja.size(); i++) {
			JsonValue jv = ja.get(i);
			if (JsonValue.ValueType.OBJECT.equals(jv.getValueType())) {
				System.out.println(buffer + "{");
				printJson(ja.getJsonObject(i), depth+1);
				System.out.println(buffer + "}");
			} else if (JsonValue.ValueType.ARRAY.equals(jv.getValueType())) {
				System.out.println(buffer + "[");
				printJson(ja.getJsonArray(i), depth+1);
				System.out.println(buffer + "]");
			} else if (JsonValue.ValueType.STRING.equals(jv.getValueType())) {
				System.out.println(buffer + "\"" + ja.getString(i).replaceAll("\n", " \\n ") + "\"");
			} else {
				System.out.println(buffer + ja.get(i).toString());
			}
		}
	}	

}
