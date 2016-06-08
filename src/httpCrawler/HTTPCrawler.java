package httpCrawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.Socket;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import dnsResolver.DNSResolver;
import urlParser.URL;

public class HTTPCrawler {
	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;

	// open stream to the host
	public HTTPCrawler(String ip, int port) throws IOException {
		if (port == 443) {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			clientSocket = (SSLSocket) sslsocketfactory.createSocket(ip, 443);
		}
		else
			clientSocket = new Socket(ip, port);
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}
	
	// close the stream
	public void Close() throws IOException {
		inFromServer.close();
		outToServer.close();
		clientSocket.close();
	}

	// send a http querry
	public String HTTPQuery(String function, String host, String res) throws IOException {
		String sentence = function + " " + res + " HTTP/1.0\r\n" + "Host: " + host + "\r\n"
				+ "User-Agent: CLIENT RIW\r\n" + "\r\n";
		outToServer.writeBytes(sentence);
		return inFromServer.readLine();
	}

	// handle redirection
	public String GetRedirectLocation() throws IOException {
		String line = null;
		while ((line = inFromServer.readLine()) != null && 0 != line.length()) {
			if (line.toUpperCase().startsWith("LOCATION"))
				return line.substring(line.indexOf(':') + 1).trim();
		}
		return null;
	}

	// parse robots file
	public void ParseRobotsFile(List<String> allow, List<String> disallow) throws IOException {
		String line = null;
		while (null != (line = inFromServer.readLine())) {
			if (line.toUpperCase().startsWith("ALLOW"))
				allow.add(line.substring(line.indexOf(':') + 1).trim());
			if (line.toUpperCase().startsWith("DISALLOW"))
				disallow.add(line.substring(line.indexOf(':') + 1).trim());
		}
	}

	public String GetHeader() throws IOException {
		StringBuilder header = new StringBuilder();
		String line = null;
		while (null != (line = inFromServer.readLine()) && 0 != line.length()) {
			header.append(line + "\r\n");
		}
		return header.toString();
	}

	public String ReadLine() throws IOException {
		return inFromServer.readLine();
	}
	
	public void WriteLogHtml(String file) throws IOException {
		Writer output = new BufferedWriter(new FileWriter(new File(file)));
		String line = null;
		while ((line = inFromServer.readLine()) != null) {
			output.write(line + "\r\n");
		}
		output.flush();
		output.close();
	}
	
	public static HTTPCrawler InitCrawler(URL url) {
		String ip;
		if (null == (ip = DNSResolver.GetIpAddress(url.getHost()))) {
			System.err.println("Could not resolve hostname to ip address");
			return null;
		}
		try {
			HTTPCrawler crawler = new HTTPCrawler(ip, url.getPort());
			String response = crawler.HTTPQuery("GET", url.getHost(), url.getResurce());

			if (response.contains("20"))
				return crawler;
			else if (response.contains("30")) {
				String redirect;
				if (null == (redirect = crawler.GetRedirectLocation())) {
					System.err.println("Could not find redirect location");
					return null;
				}
				url = URL.ParseURL(redirect);
				return InitCrawler(url);
			} else {
				return null;
			}
		} catch (IOException e) {
			System.err.print("IOException in crawler");
			return null;
		}
	}
}
