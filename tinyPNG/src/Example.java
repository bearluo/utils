import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

import com.tinify.*;
import com.tinify.Exception;

public class Example {
	public static String cachePath = "cachePath";
	public static String tinifyCachePath = "tinifyCachePath";
	public static ArrayList<String> keys = new ArrayList<String>();
	public static int keyIndex = -1;
	public static String keyPath = "keys";
	public static String srcPath = "D:\\test\\res";
	public static String outPath = "D:\\test\\res_temp";
	public static boolean isNewSrc = false;

	public static void main(String[] args) {
		try {
			Scanner sc=new Scanner(new File("config"));
			if (sc.hasNextLine()) {
				srcPath = sc.nextLine();
			}
			if (sc.hasNextLine()) {
				outPath = sc.nextLine();
			}
			if (sc.hasNextLine()) {
				isNewSrc = sc.nextLine().equals("true");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			srcPath = "";
			outPath = "";
			isNewSrc = true;
		}
		System.out.println("srcPath:"+srcPath);
		System.out.println("outPath:"+srcPath);
		System.out.println("isNewSrc:"+isNewSrc);
		readKey();
		
		if ( !fileIsExists(srcPath) ) return;
		
		if (isNewSrc) build_cache_path();
	    if (changeKey() && checkCompressionCountAndChangeKey()) {
	    	System.out.println("开始压缩");
	    	tinifyPng();
	    }else{
	    	System.out.println("免费次数用完了 请添加新key");
	    }
	}

	// 判断文件是否存在
	public static boolean fileIsExists(String strFile) {
		try {
			File f = new File(strFile);
			if (!f.exists()) {
				return false;
			}

		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public static void readKey() {
		try {
			FileInputStream is = new FileInputStream(keyPath);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader in = new BufferedReader(isr);
			String line = null;
			while ((line = in.readLine()) != null) {
				keys.add(line);
			}
			in.close();
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean checkCompressionCountAndChangeKey() {
		int compressionsThisMonth = Tinify.compressionCount();
		if (compressionsThisMonth < 500) {
			System.out.println("剩余压缩个数:" + (500 - compressionsThisMonth));
			return true;
		} else {
			System.out.println("剩余压缩个数:0");
			return changeKey() && checkCompressionCountAndChangeKey();
		}
	}

	public static boolean changeKey() {
		keyIndex++;
		try {
			if (keyIndex < keys.size() && keyIndex >= 0) {
				System.out.println("切换账号:" + keys.get(keyIndex));
				Tinify.setKey(keys.get(keyIndex));
				Tinify.validate();
				return true;
			} else {
				return false;
			}
		} catch (java.lang.Exception e) {
			return changeKey();
		}
	}

	public static void tinifyPng() {
		try {
			Map map = readTinifyCachePath();
			PrintWriter pw = new PrintWriter(new FileWriter(tinifyCachePath,
					true));
			FileInputStream is = new FileInputStream(cachePath);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader in = new BufferedReader(isr);
			String line = null;
			while ((line = in.readLine()) != null) {
				boolean isNew = map.get(line) == null;
				if (isNew) {
					System.out.println("压缩图片:" + line);
					Tinify.fromFile(line)
							.toFile(line.replace(srcPath, outPath));
					System.out.println("已压缩图片:"
							+ line.replace(srcPath, outPath));
					pw.println(line);
					pw.flush();
					if (!checkCompressionCountAndChangeKey()) {
						System.out.println("免费次数用完了");
						break;
					}
				} else {
					System.out.println("忽略压缩图片:" + line);
				}
			}
			in.close();
			is.close();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map readTinifyCachePath() {
		Map map = new HashMap();
		try {
			FileInputStream is = new FileInputStream(tinifyCachePath);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader in = new BufferedReader(isr);
			String line = null;
			while ((line = in.readLine()) != null) {
				map.put(line, line);
			}
			in.close();
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static int build_cache_path() {
		System.out.println("开始生成压缩文件配置");
		int fileNum = 0, folderNum = 0;
		File srcFile = new File(srcPath);
		LinkedList<String> cacheFile = new LinkedList<String>();
		if (srcFile.exists()) {
			LinkedList<File> list = new LinkedList<File>();
			File[] files = srcFile.listFiles();
			for (File file2 : files) {
				if (file2.isDirectory()) {
					System.out.println("文件夹:" + file2.getAbsolutePath());
					list.add(file2);
					folderNum++;
				} else if (file2.getName().endsWith(".png")
						|| file2.getName().endsWith(".jpg")) {
					System.out.println("文件:" + file2.getAbsolutePath());
					cacheFile.add(file2.getAbsolutePath());
					fileNum++;
				}
			}
			File temp_file;
			while (!list.isEmpty()) {
				temp_file = list.removeFirst();
				files = temp_file.listFiles();
				for (File file2 : files) {
					if (file2.isDirectory()) {
						System.out.println("文件夹:" + file2.getAbsolutePath());
						list.add(file2);
						folderNum++;
					} else if (file2.getName().endsWith(".png")
							|| file2.getName().endsWith(".jpg")) {
						System.out.println("文件:" + file2.getAbsolutePath());
						cacheFile.add(file2.getAbsolutePath());
						fileNum++;
					}
				}
			}
		} else {
			System.out.println("srcPath is not exists");
		}
		System.out.println("文件夹共有:" + folderNum + ",文件共有:" + fileNum);
		if (fileNum > 0) {
			try {
				@SuppressWarnings("resource")
				PrintWriter pw = new PrintWriter(new FileWriter(cachePath));
				File tinifyCacheFile = new File(tinifyCachePath);
				if (tinifyCacheFile.exists())
					tinifyCacheFile.delete();
				while (!cacheFile.isEmpty()) {
					pw.println(cacheFile.removeFirst());
					pw.flush();
				}
				pw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fileNum;
	}
}
