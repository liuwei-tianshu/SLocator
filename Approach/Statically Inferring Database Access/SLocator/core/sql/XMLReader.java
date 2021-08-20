package SLocator.core.sql;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import SLocator.datastructure.Program;
import lombok.Data;
import lombok.Getter;

/**
 * read configuration from xml files like Admin.orm.xml
 */
public class XMLReader {
	private static List<File> sqlXMLFiles = new ArrayList<File>();
	@Getter private static Map<String, List<String>> sqlMap = new HashMap<String, List<String>>();
	
	public static void run(Program program) {
		try {
			readXMLFilex(program.getPath());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void readXMLFilex(String path) throws JavaModelException, CoreException {
		/**
		 * step 1 get all sql xml file paths
		 */
		scanSqlXMLFiles(path);
		
		/**
		 * step 2 read all sql xml files
		 */
		sqlXMLFiles.forEach(xmlFile -> {
			readXMLFile(xmlFile);
		});
	}
	
	private static void scanSqlXMLFiles(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					scanSqlXMLFiles(files[i].getAbsolutePath());
				} else if (file.isFile()) {
					if (file.getPath().contains("jpa") && file.getPath().contains("domain")) {
						String[] fileNames = file.getPath().split("\\.");
						if (fileNames != null && fileNames.length >= 1 && fileNames[fileNames.length-1].equals("xml")) {
							sqlXMLFiles.add(file);
						}
					}
				}
			}
		}
	} 
	
	public static void readXMLFile(File file) {
		try {
	         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(file);
	         doc.getDocumentElement().normalize();
	         if (!doc.getDocumentElement().getNodeName().equals("entity-mappings")) {
	        	 return;
	         }

	         NodeList nList = doc.getElementsByTagName("named-query");
	         for (int temp = 0; temp < nList.getLength(); temp++) {
	            Node nNode = nList.item(temp);
	            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	               Element eElement = (Element) nNode;
              	   List<String> sqls = new ArrayList<String>();
	               NodeList sqlNodeList = eElement.getElementsByTagName("query");
	               for(int index = 0; index < sqlNodeList.getLength(); index++) {
	            	   sqls.add(removeExtraBlanks(sqlNodeList.item(index).getTextContent()));
	               }
	               sqlMap.put(eElement.getAttribute("name"), sqls);
	            }
	         }
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	}
	
	/**
	 * remove extra blanks
	 * @param str
	 * 
	 * hello,    world!  -->
	 * hello, world
	 */
	public static String removeExtraBlanks(String str) {
		String[] strArray = str.trim().replaceAll("\r", "").replaceAll("\n", "").split("\\s");
		
		
		String result = "";
		if (strArray != null) {
			for (int i = 0; i < strArray.length; i++) {
				if (!strArray[i].isEmpty()) {
					result += " " + strArray[i];
				}
			}
			return result.trim();
		} else {
			return null;
		}
	}
	
}
