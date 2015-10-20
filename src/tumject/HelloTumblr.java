package tumject;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.*;

import mtg.CardFilter;
import mtg.FilterIsRealCard;
import mtg.FilterSupplemental;
import mtg.MtgJson;
import mtg.MtgJson.Spread;

import java.util.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;

public class HelloTumblr {
	public static void main(String[] args) throws IOException {
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

		dumpFollowing(prop, client, false);

		/*
		User user = client.user();
		System.out.println(user.getName());
		*/

//		Blog blog = client.blogInfo(prop.getProperty("tumblr.blogname")); //.tumblr.com");
//		dumpPosts(blog);
//		dumpQueue(client.blogInfo(prop.getProperty("tumblr.blogname")));

		postTarot(prop, client, Spread.THREE_CARD, 1);
	}

	// Gotta replace — with &mdash; .  Probably others too.

	public static void dumpQueue(Blog blog) {
		dumpObject(blog);
//		Map<String, String> options = new HashMap<String, String>();
		List<Post> queue = blog.queuedPosts();
		System.out.println();
		System.out.println("Queue size: " + queue.size());
		boolean dumped = false;
		for (Post p : queue) {
			if (!dumped) {
				dumpObject(p);
				dumped = true;
			}
			Date timestamp = new Date();
			timestamp.setTime(1000l * p.getTimestamp());
			System.out.println(" Post: ");
			System.out.println("  Timestamp: " + timestamp);
			System.out.println("  DateGMT: " + p.getDateGMT());
		}
		
		System.out.println();
		List<Post> posts = blog.posts();
		for (Post p : posts) {
			dumpObject(p);
			Date timestamp = new Date();
			timestamp.setTime(1000l * p.getTimestamp());
			System.out.println(" Post: ");
			System.out.println("  Timestamp: " + timestamp);
			System.out.println("  DateGMT: " + p.getDateGMT());
			break;
		}
	}

	public static void postTarot(Properties prop, JumblrClient client, Spread spread) {
		Blog blog = client.blogInfo(prop.getProperty("tumblr.blogname"));
		List<Post> queue = blog.queuedPosts();
		System.out.println("Queue length: " + queue.size());
	}
	
	public static void postTarot(Properties prop, JumblrClient client, Spread spread, int numberOfPosts) {
		try {
			MtgJson mj = new MtgJson(prop.getProperty("mtg.directory"), prop.getProperty("background.image"));
			List<String> tags = new ArrayList<String>();
			tags.add("tarot");
			tags.add("magic the gathering");
			tags.add("mtg");
			
			for (int i=0; i<numberOfPosts; i++) {
				PhotoPost post = client.newPost(prop.getProperty("tumblr.blogname"), PhotoPost.class);
				//TODO: Figure out what day this will be posted, change spread and filters accordingly
				Random r = new Random();
				int j = r.nextInt(numberOfPosts);
				if (i==j) {
					mj.setFirstFilters(new CardFilter[]{new FilterIsRealCard(), new FilterSupplemental(null, prop.getProperty("mtg.directory"))});
				} else {
					mj.setFirstFilters(null);
				}
				mj.setSpread(spread);

				j = r.nextInt(numberOfPosts);
				if (i==j) {
					mj.setSpread(Spread.CELTIC_CROSS);
				} else {
					mj.setSpread(Spread.THREE_CARD);
				}

				mj.generatePost();
				
				post.setCaption(mj.getCaption());
				post.setPhoto(new Photo(new File(mj.getImageFilename())));
				post.setTags(tags);
				post.setState("queue");

				System.out.println(mj.getCaption().replaceAll("\\n", "</p><p>"));
				System.out.println(mj.getImageFilename());

				post.save();
			}
		} catch (IllegalAccessException | InstantiationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void dumpFollowing(Properties prop, JumblrClient client, boolean verbose) {
		int maxTries = 99;
		
		List<Blog> following = new ArrayList<Blog>();
		List<Blog> tFollowing = client.userFollowing();
		while (tFollowing!=null && tFollowing.size()>0 && maxTries>0) {
			following.addAll(tFollowing);
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("offset", following.size());
			tFollowing = client.userFollowing(options);
			maxTries--;
//			System.out.println("following = " + following.size() + ", tFollowing = " + tFollowing.size() + ", offset = " + options.get("offset") + ", tries = " + maxTries);
		}
		Collections.sort(following, new Comparator<Blog>(){
			@Override
			public int compare(Blog blog1, Blog blog2) {
				return blog1.getName().compareToIgnoreCase(blog2.getName());
			}});

		try {
			java.text.SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			PrintWriter pw = new PrintWriter(prop.getProperty("tumblr.dir") + "/following_" + sdf.format(new Date()) + ".txt");
			
			int i=1;
			long now = new Date().getTime()/1000l;
			for (Blog blog : following) {
				long lastPost = now - blog.getUpdated();
				String lastPostStr = lastPost + " sec";
				if (lastPost>=60) {
					lastPost/=60;
					lastPostStr = lastPost + " min";
				}
				if (lastPost>=60) {
					lastPost/=60;
					lastPostStr = lastPost + " hr";
				}
				if (lastPost>=24) {
					lastPost/=24;
					lastPostStr = lastPost + " day";
				}
				if (lastPost>=365) {
					lastPost/=365;
					lastPostStr = lastPost + " year*";
				} else if (lastPost>=30) {
					lastPost/=30;
					lastPostStr = lastPost + " mon*";
				}

				pw.println(i + ": " + blog.getName() + " (" + blog.getTitle() + "; " + lastPostStr + ")");
				if (verbose) System.out.println(blog.getName() + " (" + blog.getTitle() + "; " + lastPostStr + ")");
				i++;
			}
			
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void dumpPosts(Blog blog) {
		List<Post> posts = blog.posts();
		System.out.println(posts.size());
		for (Post p : posts) {
			dumpObject(p);
		}
		
	}
	
	public static void dumpObject(Object o) {
		System.out.println(o.toString());
		Method[] methods = o.getClass().getMethods();
		for (Method m : methods) {
			if ( (m.getName().startsWith("get") || m.getName().startsWith("is")) && m.getParameterCount()==0)
				try {
					Object o2 = m.invoke(o);
					if (o2==null) {
						System.out.println(" " + m.getName() + " returned null");
					} else {
						System.out.println(" " + m.getName() + " returned a " + o2.getClass().getName() + ": " + o2);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	public static void main0(String[] args) throws IOException {
		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));
		
		JumblrClient client = new JumblrClient(
				  prop.getProperty("tumblr.key.1"),
				  prop.getProperty("tumblr.key.2")
				);

		Blog blog = client.blogInfo(prop.getProperty("tumblr.blogname"));
		System.out.println("Posts: " + blog.getPostCount());
		Map<String, String> options = new HashMap<String, String>();
		options.put("offset", Integer.toString(blog.getPostCount()-2));
		List<Post> l = blog.posts(options);
		for (Post p : l) {
			dumpObject(p);
		}
	}

}
