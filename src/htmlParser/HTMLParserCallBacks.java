package htmlParser;

import java.util.Arrays;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import urlParser.URL;

class HTMLParserCallBacks extends HTMLEditorKit.ParserCallback {
	private HTML.Tag currentTag = null;
	public HTMLPage page = null;

	public HTMLParserCallBacks(URL url) {
		this.page = new HTMLPage(url);
	}

	public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
		this.handleTag(tag, attributes);
	}

	public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
		this.handleTag(tag, attributes);
	}
	
	public void handleTag(HTML.Tag tag, MutableAttributeSet attributes) {
		currentTag = tag;
		Object attribute = null;	
		if (tag == HTML.Tag.META) {
			attribute = attributes.getAttribute(HTML.Attribute.NAME);
			if (attribute != null) {
				String key = attribute.toString();
				attribute = attributes.getAttribute(HTML.Attribute.CONTENT);
				if (attribute != null) {
					if (key.equalsIgnoreCase("KEYWORDS") || key.equalsIgnoreCase("DESCRIPTION")) {
						page.words.addAll(Arrays.asList(attribute.toString().split("[\\W]")));
					}
					if (key.equalsIgnoreCase("ROBOTS")) {
						String value = attribute.toString().toUpperCase();
						page.toFollow = !value.contains("NOFOLLOW");
						page.toIndex = !value.contains("NOINDEX");
					}
				}
			}
		}	
		if (tag == HTML.Tag.A) {
			attribute = attributes.getAttribute(HTML.Attribute.REL);
			if (attribute == null || !attribute.toString().equalsIgnoreCase("NOFOLLOW")) {
				attribute = attributes.getAttribute(HTML.Attribute.HREF);
				if (attribute != null && attribute.toString().indexOf('#') < 0) {
					try {
						page.links.add(page.url.rezolve(attribute.toString()).toString());
					} catch(Exception e){}
				}
			}
		}
	}

	public void handleText(char[] data, int position) {
		if (currentTag == HTML.Tag.TITLE)
			for (String word : new String(data).toLowerCase().split("\\W+"))
				page.words.add(word);
	}
}
