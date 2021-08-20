package SLocator.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileUtil {
	public FileUtil() {

	}

	public static PrintWriter getPrintWriter(String filePath) {
		try {
			File file = new File(filePath);

			if (!file.exists()) {
				file.createNewFile();
			}
			
			PrintWriter pw = new PrintWriter(new FileWriter(file));
			return pw;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static BufferedReader getFileReader(String filePath) {
		try {
			BufferedReader bf = new BufferedReader(new FileReader(filePath));
			return bf;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static BufferedWriter getBufferedWriter(String filePath) {
		try {
			File file = new File(filePath);

			if (!file.exists()) {
				file.createNewFile();
			}

			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			// do stuff
			return writer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
