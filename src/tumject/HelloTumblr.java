package tumject;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.*;

import mtg.filter.*;
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
	public static void main01(String[] args) throws IOException {
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

		Blog blog = client.blogInfo("elspethsunschampion");
		dumpObject(blog);
		Long lastPostTimestamp = null;
		List<Post> posts = blog.posts();
		int i=0;
		Map<String, Object> options = new HashMap<String, Object>();
		
		for (int j=0; j<2; j++) {
			for (Post post : posts) {
				System.out.println( (i++) + "\t" + post.getId() + "\t" + post.getDateGMT() + "\t" + post.getTimestamp() + "\t" + (lastPostTimestamp==null?"":lastPostTimestamp-post.getTimestamp()));
				lastPostTimestamp = post.getTimestamp();
			}
			options.put("offset", i);
			posts = blog.posts(options);
		}
	}
	
	public static void main(String[] args) throws IOException {
		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));
		
		// Authenticate via OAuth
		JumblrClient client = new JumblrClient(
		  prop.getProperty("tumblr.key.1"), // API Key
		  prop.getProperty("tumblr.key.2")  // API Secret
		);

		client.setToken(
		  prop.getProperty("tumblr.key.3"), // Token Key
		  prop.getProperty("tumblr.key.4")  // Token Secret
		); 

		try {
			dumpFollowing(prop, client, false);
		} catch (JumblrException e) {
			System.out.println(e);
		}

		/*
		User user = client.user();
		System.out.println(user.getName());
		*/

//		Blog blog = client.blogInfo(prop.getProperty("tumblr.blogname")); //.tumblr.com");
//		dumpPosts(blog);
//		dumpQueue(client.blogInfo(prop.getProperty("tumblr.blogname")));

//		postTarot(prop, client);
		postTarot(prop, client, Spread.THREE_CARD, 5);
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
	
	public static void postTarot(Properties prop, JumblrClient client) {
		try {
			MtgJson mj = new MtgJson(prop.getProperty("mtg.directory"), prop.getProperty("background.image"));
			List<String> tags = new ArrayList<String>();
			tags.add("tarot");
			tags.add("magic the gathering");
			tags.add("mtg");

			PhotoPost post = client.newPost(prop.getProperty("tumblr.blogname"), PhotoPost.class);

			mj.setFirstFilters(new CardFilter[]{new FilterSet("BFZ", "OGW", "EXP").setDescription("Battle for Zendikar block"), new FilterCardType("Enchantment", "Artifact", "Land").setDescription("enchantment, artifact, and land"), new FilterIsRealCard()});
			mj.setSpread(Spread.CELTIC_CROSS);
//			mj.setSpread(Spread.THREE_CARD);

			mj.generatePost();

			post.setCaption(mj.getCaption());
			post.setPhoto(new Photo(new File(mj.getImageFilename())));
			post.setTags(tags);
			post.setState("queue");

			System.out.println(mj.getCaption().replaceAll("\\n", "</p><p>"));
			System.out.println(mj.getImageFilename());

			post.save();

		} catch (IllegalAccessException | InstantiationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void postTarot(Properties prop, JumblrClient client, Spread spread, int numberOfPosts) {
		try {
			MtgJson mj = new MtgJson(prop.getProperty("mtg.directory"), prop.getProperty("background.image"));
			List<String> tags = new ArrayList<String>();
			tags.add("tarot");
			tags.add("magic the gathering");
			tags.add("mtg");

			Random r = new Random();
			int sup1 = r.nextInt(Math.min(5, numberOfPosts)), sup2 = r.nextInt(Math.min(5, numberOfPosts));
			int cel1 = r.nextInt(Math.min(5, numberOfPosts)), cel2 = r.nextInt(Math.min(5, numberOfPosts));
			sup1 = -1; cel1 = -1; 

			for (int i=0; i<numberOfPosts; i++) {
				PhotoPost post = client.newPost(prop.getProperty("tumblr.blogname"), PhotoPost.class);
				//TODO: Figure out what day this will be posted, change spread and filters accordingly
				if (i==sup1 || i==sup2) {
					mj.setFirstFilters(MtgJson.choosePresetFilters(prop));
//					mj.setFirstFilters(new CardFilter[]{new FilterVanilla(), new FilterIsRealCard()});
//					mj.setFirstFilters(new CardFilter[]{new FilterIsRealCard(), new FilterSupplemental(null, prop.getProperty("mtg.directory"))});
				} else {
					mj.setFirstFilters(null);
				}
				mj.setSpread(spread);

				if (i==cel1 || i==cel2) {
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
				if (blog1.getUpdated()>blog2.getUpdated()) return 1;
				if (blog1.getUpdated()<blog2.getUpdated()) return -1;
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

				pw.println(i + "\t" + blog.getName() + "\t" + blog.getTitle() + "\t" + lastPostStr);
				if (verbose) System.out.println(blog.getName() + "\t" + blog.getTitle() + "\t" + lastPostStr);
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
