package webRobot;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import htmlParser.HTMLPage;
import htmlParser.HTMLParser;
import httpCrawler.HTTPCrawler;
import urlParser.URL;

public class WebAgent implements Runnable {

	private ConcurrentLinkedQueue<String> _toExplore;
	private List<String> _allow = new LinkedList<>();
	private List<String> _disallow = new LinkedList<>();
	private List<HTMLPage> _data;
	private boolean _suspended = false, _stoped = false;
	private static String _cacheDir;

	public WebAgent(ConcurrentLinkedQueue<String> toExplore, List<HTMLPage> data, String cacheDir){
		_toExplore = toExplore;
		_data = data;
		_cacheDir = cacheDir;
	}
	
	public void Stop()
	{
		_stoped = true;
	}

	public void Suspend()
	{
		_suspended = true;
	}

	synchronized public void Resume()
	{ 
		_suspended = false;
		notify();
	}
	
	@Override
	public void run() {
		while(!_stoped) 
		{ 
			try 
			{ 				
				synchronized(this) 
				{ 
					if(_suspended) 
						wait();
				}
				
				String uri = _toExplore.poll();
				if (null == uri)
					continue;				
				URL url = URL.ParseURL(uri);
				_allow.clear();
				_disallow.clear();
				try {
					HTTPCrawler crawler;
					if (null != (crawler = HTTPCrawler.InitCrawler(URL.ParseURL(url.getHost() + "/robots.txt")))) {
						crawler.ParseRobotsFile(_allow, _disallow);
						crawler.Close();
					}					
					//TO DO: check if allowed
					if (null == (crawler = HTTPCrawler.InitCrawler(url))) {
						continue;
					}
					crawler.GetHeader();
					String dirPath = _cacheDir + "/" + url.getHost() + url.getPath();
					new File(dirPath).mkdirs();
					String filePath = dirPath + "/" + url.hashCode();
					crawler.WriteLogHtml(filePath);
					crawler.Close();
					
					HTMLParser parser = new HTMLParser();
					HTMLPage pageInfo = parser.Parse(url, new FileReader(filePath));
					pageInfo.cachedFile = filePath;
					System.out.println(pageInfo.url.toString() + " " + pageInfo.words);
					_toExplore.addAll(pageInfo.links);
					_data.add(pageInfo);				
				} catch (IOException e) {
					System.err.println("IOException in web agent");
				}
			} 
			catch(InterruptedException e)
			{
				System.err.println("Web agent interrupted");
			} 
		}
	}
}
