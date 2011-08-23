package opengraph;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

/**
 * A Java object representation of an Open Graph enabled webpage
 * A simplified layer over a HashMap
 * @author Callum Jones
 */
public class OpenGraphReader {
    private final static String[] REQUIRED_META = new String[]{"title", "type", "image", "url" };
    
    private boolean ignoreSpecificationErrors;
    private boolean mineExtraInformation;
    
    public OpenGraphReader() {
    	ignoreSpecificationErrors = true;
    	mineExtraInformation = true;
    }
    
    public OpenGraphReader(final boolean ignoreSpecificationErrors, final boolean mineExtraInformation) {
    	this.ignoreSpecificationErrors = ignoreSpecificationErrors;
    	this.mineExtraInformation = mineExtraInformation;
    }
    
    /**
     * Fetch the open graph representation from a web site
     * @param url The address to the web page to fetch Open Graph data
     * @param ignoreSpecificationErrors Set this option to true if you don't wish to have an exception throw if the page does not conform to the basic 4 attributes
     * @throws java.io.IOException If a network error occurs, the HTML parser will throw an IO Exception
     * @throws java.lang.Exception A generic exception is throw if the specific page fails to conform to the basic Open Graph standard as define by the constant REQUIRED_META
     */
    public OpenGraph read(final String url) throws java.io.IOException, Exception {
    	final TagNode nodes = new HtmlCleaner().clean(new URL(url));
        final HashMap<String, String> properties = new HashMap<String, String>();
        
        extractOpenGraphDataFromMetaTags(nodes.getElementsByName("meta", true), properties);
        checkSpecificationConformity(properties);
        determineType(properties);
        if (mineExtraInformation) {
        	checkTitle(nodes, properties);
        	checkDescription(nodes, properties);
//        	checkImage(nodes, properties);
        }
        
        return new OpenGraph(url, properties);
    }

	private void extractOpenGraphDataFromMetaTags(final TagNode[] metaData, final HashMap<String, String> properties) {
		for (TagNode metaElement : metaData) {
            if (metaElement.hasAttribute("property") && metaElement.getAttributeByName("property").startsWith("og:")) {
            	properties.put(metaElement.getAttributeByName("property"), StringEscapeUtils.unescapeHtml4(metaElement.getAttributeByName("content")));
            } else if (mineExtraInformation && metaElement.hasAttribute("name")) {
                properties.put(metaElement.getAttributeByName("name"), StringEscapeUtils.unescapeHtml4(metaElement.getAttributeByName("content")));
            }
        }
	}

	private void checkSpecificationConformity(final HashMap<String, String> properties) throws Exception {
		if (!ignoreSpecificationErrors) {
            for (String requiredProperties : REQUIRED_META) {
                if (properties.containsKey(requiredProperties)) {
                    throw new Exception("Does not conform to Open Graph protocol");
                }
            }
        }
	}

	private static void determineType(final HashMap<String, String> properties) {
		final String type = properties.get("og:type");
		if (type == null) {
			properties.put("type", "page");
		}
	}

//	private static void checkImage(final TagNode pageData, final OpenGraph openGraph) {
//		if (StringUtils.isBlank(getProperty("image"))) {
//			openGraph.getProperties().put("image", "http://www.getfavicon.org/?url=" + domain + "/favicon.30.png");
//		}
//	}

	private void checkDescription(final TagNode nodes, final Map<String, String> properties) {
		if (StringUtils.isBlank(properties.get("description"))) {
			mineDescription(nodes, properties);
		}
	}

	private void mineDescription(final TagNode nodes, final Map<String, String> properties) {
		if (StringUtils.isBlank(properties.get("description"))) {
			searchDescriptionInPTags(nodes, properties);
		}
		
		if (StringUtils.isBlank(properties.get("description"))) {
			searchDescriptionInDivTags(nodes, properties);
		}
	}

	private void searchDescriptionInDivTags(final TagNode nodes, final Map<String, String> properties) {
		@SuppressWarnings("unchecked")
		List<TagNode> tags = nodes.getElementListByName("div", true);
		for (TagNode tag : tags) {
			properties.put("description", StringEscapeUtils.unescapeHtml4(tag.getText().toString()));
			break;
		}
	}

	private void searchDescriptionInPTags(final TagNode nodes, final Map<String, String> properties) {
		@SuppressWarnings("unchecked")
		List<TagNode> tags = nodes.getElementListByName("p", true);
		for (TagNode tag : tags) {
			properties.put("description", StringEscapeUtils.unescapeHtml4(tag.getText().toString()));
			break;
		}
	}

	private void checkTitle(final TagNode nodes, final Map<String, String> properties) {
		if (StringUtils.isBlank(properties.get("title"))) {
			mineTitle(nodes, properties);
    	}
	}

	private void mineTitle(final TagNode nodes, final Map<String, String> properties) {
		searchTitleTags(nodes, properties);
		if (StringUtils.isBlank(properties.get("title"))) {
			searchH1Tags(nodes, properties);
		}
	}
	
	private void searchH1Tags(final TagNode nodes, final Map<String, String> properties) {
		@SuppressWarnings("unchecked")
		final List<TagNode> tags = nodes.getElementListByName("h1", false);
		for (TagNode tag : tags) {
			if (StringUtils.isNotBlank(tag.getText())) {
				properties.put("title", StringEscapeUtils.unescapeHtml4(tag.getText().toString()));
				return;
			} 
		}
	}

	private void searchTitleTags(final TagNode nodes, final Map<String, String> properties) {
		@SuppressWarnings("unchecked")
		final List<TagNode> tags = nodes.getElementListByName("title", true);
		for (TagNode tag : tags) {
			if (StringUtils.isNotBlank(tag.getText())) {
				properties.put("title", tag.getText().toString());
				return;
			} 
		}
	}
	
    public boolean isIgnoreSpecificationErrors() {
		return ignoreSpecificationErrors;
	}

	public void setIgnoreSpecificationErrors(boolean ignoreSpecificationErrors) {
		this.ignoreSpecificationErrors = ignoreSpecificationErrors;
	}

	public boolean isMineExtraInformation() {
		return mineExtraInformation;
	}

	public void setMineExtraInformation(boolean mineExtraInformation) {
		this.mineExtraInformation = mineExtraInformation;
	}

	public static void main(String[] args) throws IOException, Exception {
		OpenGraphReader reader = new OpenGraphReader();
		OpenGraph openGraph = reader.read("http://slatest.slate.com/posts/2011/08/21/rick_perry_s_texas_jobs_boom_comes_from_big_government.html");
		for (Entry<String, String> entry : openGraph.getProperties().entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
	}
}