import java.io.IOException;

import com.tinify.*;


public class Example {
	public static void main(String[] args) {
	    Tinify.setKey("afiBbemUA-bCjDLrD9SKoNxXduQOpEEA");
	    try {
			Tinify.fromFile("psb.jpg").toFile("test.jpg");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
