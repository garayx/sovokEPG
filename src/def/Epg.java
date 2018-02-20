package def;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Epg {
	private String LOGIN;
	private String PASSWORD;
	private String HOURS;
	private String PATH;
	private static List<String> ids = new ArrayList<String>();
	private boolean makeLogos;
	
	public static class Builder {
		private String LOGIN = new String();
		private String PASSWORD = new String();
		private String HOURS = "24";
		private String PATH = new String();
		private boolean makeLogos = false;
		
		public Builder () {}
		
		public Builder LOGIN(String LOGIN) {
            this.LOGIN = LOGIN;
            return this;
        }
		public Builder PASSWORD(String PASSWORD) {
            this.PASSWORD = PASSWORD;
            return this;
        }
		public Builder HOURS(String HOURS) {
            this.HOURS = HOURS;
            return this;
        }
		public Builder PATH(String PATH) {
            this.PATH = PATH;
            return this;
        }
		public Builder makeLogos(boolean makeLogos) {
            this.makeLogos = makeLogos;
            return this;
        }
		
		public Epg build() {
			return new Epg(LOGIN,
					PASSWORD,
					HOURS,
					PATH,
					makeLogos);
		}
		
		
		
	}
		public Epg(String LOGIN,
				String PASSWORD,
				String HOURS,
				String PATH,
				boolean makeLogos){
			this.LOGIN = LOGIN;
			this.PASSWORD = PASSWORD;
			this.HOURS = HOURS;
			this.PATH = PATH;
			this.makeLogos = makeLogos;
			final long START = (((new Date().getTime())/1000)-3600);
			File finaloutput = new File(PATH+"\\final.xml");
			
			//temp file saved at userhome, deleted afterwards
			File output = new File(System.getProperty("user.home"), "output.txt");
			//output.deleteOnExit();
			
			try{
				String pre_loginurl = "http://api.sovok.tv/v2.3/xml/login?login="+LOGIN+"&pass="+PASSWORD;
				String pre_apiURL = "http://api.sovok.tv/v2.3/xml/epg3?dtime="+START+"&period="+HOURS;
				URL loginurl = new URL(pre_loginurl);
				URL url = new URL(pre_apiURL);
				CookieManager cookieManager = new CookieManager();
				CookieHandler.setDefault(cookieManager);
				HttpURLConnection login = (HttpURLConnection)loginurl.openConnection();
				login.getContent();
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(url.openStream());
				//run the funcs
				printDocument(doc, output);
				transformDocument(output,finaloutput, HOURS);
				if(makeLogos)
					mkLogos(PATH);
				
			}catch(Exception e){
				System.out.println(e);
			}
		}

		
		private static void transformDocument(File doc, File out, String strHr) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException, TransformerException{
			final int BYTES = 16000; //num of BYTES offset for description
			Date startDate = new Date();
			int hr = Integer.parseInt(strHr);
			Date endDate = new Date(startDate.getTime() + TimeUnit.HOURS.toMillis(hr));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+3"));
			String formattedendDate = sdf.format(endDate);
			
			//new InputStreamReader(new FileInputStream(filePath), encoding)
			//try (BufferedReader br = new BufferedReader(new FileReader(doc))) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(doc), "UTF-8"))){
			    String line;
			    PrintWriter outputstream = new PrintWriter(out);
		    	outputstream.println("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
		    	outputstream.println("<!DOCTYPE tv SYSTEM \"http://www.teleguide.info/download/xmltv.dtd\">");
		    	outputstream.println("<tv generator-info-name=\"TVH_W/0.751l\" generator-info-url=\"http://www.teleguide.info/\">");

		    	int flag1=0;
		    	int flag2=0;
//		    	int size=0;
				String id = null;
				String name = null;
				String preId="<channel id=\"";
				String afterId="\">";
				String preName="<display-name lang=\"ru\">";
				String afterName="</display-name>";
			    while ((line = br.readLine()) != null) {
			    	if(line.contains("<id>")){
			    		flag1=1;
			    		int start = line.indexOf("<id>");
			    		int stop = line.indexOf("</id>");
			    		id = line.substring(start+4, stop);
			    		ids.add(id);
			    	}
			    	if(line.contains("<name>")){
			    		flag2=1;
			    		int start = line.indexOf("<name>");
			    		int stop = line.indexOf("</name>");
			    		byte bytes[] = line.substring(start+6, stop).getBytes("UTF-8");
			    		name = new String(bytes, "UTF-8");
			    	}
			    	if(flag1 == 1 && flag2 == 1){
			    	outputstream.println(preId+id+afterId);
			    	outputstream.println(preName+name+afterName);
			    	outputstream.println("</channel>");
			    	flag1=0;
			    	flag2=0;
			    	}
			    }
		    	outputstream.close();
		    	br.close();
			}catch(Exception e){
				System.out.println(e);
			}
			
			/* read id, start, end, programme, title and desc(if there is).
			 * find <id> and copy it to string, make 4 arrays of strings (start time(need to convert), programe, description)
			 * 
			 * 
			 * 
			 * */
			try (BufferedReader br = new BufferedReader(new FileReader(doc))) {
			    String line;
			    String id = null;
			    List<String> program = new ArrayList<String>();
			    List<String> startTime = new ArrayList<String>();
			    List<String> desc = new ArrayList<String>();
			    int flag=0;
			    PrintWriter outputstream = new PrintWriter(new FileWriter(out, true));
			    while ((line = br.readLine()) != null) {
			    	if(line.contains("<id>")){
			    		if(flag==0){
			    			int start = line.indexOf("<id>");
			    			int stop = line.indexOf("</id>");
			    			id = line.substring(start+4, stop);
			    			flag = 1;
			    		}else{
			    			formatDate(startTime);
			    			String zero ="0";
			    			startTime.add(formattedendDate);
			    			for(int i=0; i<startTime.size()-1;i++){
				    			outputstream.println("<programme start=\""+startTime.get(i)+" +0300"+"\" stop=\""+startTime.get(i+1)+" +0300"+"\" channel=\""+id+"\">");
				    			outputstream.println("<title lang=\"ru\">"+program.get(i)+"</title>");
				    			if(!(desc.get(i).equals(zero))){
				    				outputstream.println("<desc lang=\"ru\">"+desc.get(i)+"</desc>");
				    			}
				    			outputstream.println("</programme>");
				    			
			    			}
			    			int start = line.indexOf("<id>");
			    			int stop = line.indexOf("</id>");
			    			id = line.substring(start+4, stop);
			    			
			    		    program = new ArrayList<String>();
			    		    startTime = new ArrayList<String>();
			    		    desc=new ArrayList<String>();
			    		}
			    	}
			    	if(line.contains("<ut_start>")){
			    		int start = line.indexOf("<ut_start>");
			    		int stop = line.indexOf("</ut_start>");
			    		startTime.add(line.substring(start+10, stop));
			    	}
			    	if(line.contains("<progname>")){
			    		int start = line.indexOf("<progname>");
			    		int stop = line.indexOf("</progname>");
			    		byte bytes[] = line.substring(start+10, stop).getBytes("UTF-8");
			    		program.add(new String(bytes, "UTF-8"));
			    	}
			    		if(line.contains("<description/>")){
				    		desc.add("0");//add <description/> to file if zero
			    		}
			    		if(line.contains("<description>")){
			    			char[] cbuf = new char[BYTES];
			    			br.mark(BYTES+8); 
			    			br.read(cbuf, 0, BYTES);
			    			char[] buffer = concat(line.toCharArray(),cbuf);
			    			String temp2 = new String(buffer);
			    			String temp = temp2.replaceAll("&#13;", " ");
			    			byte bytes[] = temp.substring(temp.indexOf("<description>")+13, (temp.indexOf("</description>"))).getBytes("UTF-8");
			    			desc.add(new String(bytes, "UTF-8"));
			    			br.reset();
			    		//}
			    	}
			    }
			    outputstream.print("</tv>");
			    outputstream.close();
			    br.close();
			}catch(Exception e){
				System.out.println(e);
			}
		}
		private static char[] concat(char[] a, char[] b) {
			   int aLen = a.length;
			   int bLen = b.length;
			   char[] c= new char[aLen+bLen];
			   System.arraycopy(a, 0, c, 0, aLen);
			   System.arraycopy(b, 0, c, aLen, bLen);
			   return c;
			}
		
		private static void formatDate(List<String> arr){
			for(int i=0; i< arr.size(); i++){
				long k = Long.parseLong(arr.get(i));
				Date date = new Date(k*1000L);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				//change time zones
				sdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
				String formattedDate = sdf.format(date);
				arr.set(i,formattedDate);
				
			}
		}
		
		private static void printDocument(Document doc, File out) throws IOException, TransformerException {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			//write to file so russian lang is kept
			transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(new FileOutputStream(out), "UTF-8")));
		}
		private void mkLogos(String PATH) {
			//option to overwrite existing files
			boolean overWrite = false;
			//if folder doesnt exist, make it
			new File(PATH + "\\Logos").mkdir();
			try{
				for(int i=0; i<ids.size(); i++){
					if(overWrite){
						URL website = new URL("http://sovok.tv/logos/"+ ids.get(i) +".png");
						ReadableByteChannel rbc = Channels.newChannel(website.openStream());
						FileOutputStream fos = new FileOutputStream(PATH+ "\\Logos\\" + ids.get(i) +".png");
						fos.getChannel().transferFrom(rbc, 0, 131072); //max size 128KB
					}else{
						File f = new File(PATH+ "\\Logos\\" + ids.get(i) +".png");
						if(!f.exists() && !f.isDirectory()) { 
							URL website = new URL("http://sovok.tv/logos/"+ ids.get(i) +".png");
							ReadableByteChannel rbc = Channels.newChannel(website.openStream());
							FileOutputStream fos = new FileOutputStream(PATH+ "\\Logos\\" + ids.get(i) +".png");
							fos.getChannel().transferFrom(rbc, 0, 131072); //max size 128KB
						}
					}
					//FileOutputStream fos = new FileOutputStream(PATH+ "\\Logos\\" + ids.get(i) +".png");
					
					//fos.getChannel().transferFrom(rbc, 0, 131072); //max size 128KB
				}
			}catch(Exception e){
				System.out.println(e);
			}
		}
		
}
