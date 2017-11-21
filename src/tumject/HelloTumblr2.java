package tumject;

import java.util.List;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.Post;

public class HelloTumblr2 {

	public static void main(String[] args) {
		// Authenticate via OAuth
		JumblrClient client = new JumblrClient(
		  "8x0nvySJh9GLRKlSIWXz2RtF7MBRDo5kyAcXIeemWRfnRluiL6",
		  "MPkTf8OmgwbMADEJghzeEiz7j4Lzlr4X6cmYAvjYJ1z90KNn52"
		);
		client.setToken(
		  "Q0GJ5V2VnXWUfPUUfSxcvVAdAGSonJKnPuZzgK9mR3h8AlM8C8",
		  "1S7mytNxvHKRWMCzNcA4BOLzb6xnFTOtiWF65S3IwRIxMksT6c"
		);

		// Make the request
		List<Blog> blogs = client.userFollowing();
		for (Blog blog : blogs) {
			System.out.println(blog);
		}
	}

}
