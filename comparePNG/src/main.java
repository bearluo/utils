import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;


public class main {
	public static void main(String[] args) {
		String path = "D:/git/work/boyaa_chess/release/xiangqi_android/Resource/images";
		ArrayList<File> files = new ArrayList<>();
		ArrayList<String> out = new ArrayList<>();
		System.out.println("文件开始扫描...");
		obtainFiles(files,path);
		System.out.println("文件扫描完成");
		System.out.println("文件开始对比...");
		comparePNG(files,out);
		System.out.println("文件对比完成");
		System.out.println("");
		System.out.println("");
		print(out);
	}
	
	public static void print(ArrayList<String> out) {
		System.out.println("同样文件路径:");
		Iterator<String> iterator = out.iterator();
		while(iterator.hasNext()) {
			System.out.println(iterator.next());
		}
	}
	
	public static void obtainFiles(ArrayList<File> files,String path) {
		File file = new File(path);
		if ( !file.exists() ) return ;
		if ( file.isDirectory() ) {
			for(File subFile : file.listFiles()) {
				if ( !subFile.exists() ) continue;
				if ( subFile.isDirectory() ) {
					obtainFiles(files,subFile.getPath());
				}else if ( subFile.isFile() ) {
					files.add(subFile);
				}
			}
		}
	}
	
	public static void comparePNG(ArrayList<File> files,ArrayList<String> out) {
		for (int i=0;i<files.size();i++ ) {
			File file1 = files.get(i);
			String path1 = file1.getAbsolutePath();
			System.out.println("对比文件:" + path1);
			for (int j=i+1;j<files.size();j++ ) {
				File file2 = files.get(j);
				String path2 = file2.getAbsolutePath();
				if(file1.length() == file2.length() && getFileMD5(file1).equals(getFileMD5(file2))) {
					out.add(path1 + " equally " + path2);
				}
			}
		}
			
	}
	
	public static String getFileMD5(File file) {
	    if (!file.isFile()){
	      return null;
	    }
	    MessageDigest digest = null;
	    FileInputStream in=null;
	    byte buffer[] = new byte[1024];
	    int len;
	    try {
	      digest = MessageDigest.getInstance("MD5");
	      in = new FileInputStream(file);
	      while ((len = in.read(buffer, 0, 1024)) != -1) {
	        digest.update(buffer, 0, len);
	      }
	      in.close();
	    } catch (Exception e) {
	      e.printStackTrace();
	      return null;
	    }
	    BigInteger bigInt = new BigInteger(1, digest.digest());
	    return bigInt.toString(16);
	  }
}
