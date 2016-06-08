package htmlParser;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.html.HTMLEditorKit;

import urlParser.URL;

public class HTMLParser {
	private HTMLEditorKit.Parser parser;
	
	public HTMLParser() {
		if (parser != null)
			return;
		HTMLParserGetter kit = new HTMLParserGetter();
		parser = kit.getParser();
	}
	
	public HTMLPage Parse(URL url, Reader reader) throws IOException {
		HTMLParserCallBacks callback = new HTMLParserCallBacks(url);
		parser.parse(reader, callback, true);
		return callback.page;
	}
}
