package opengraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

/**
 * A Java object representation of an Open Graph enabled webpage
 * A simplified layer over a HashMap
 * @author Callum Jones
 */
public class OpenGraph {
    private String pageUrl;
    private HashMap<String, String> metaAttributes;
    private String baseType;
    private boolean isImported; //determine if the object is a new incarnation or representation of a web page
    private boolean hasChanged; //track if object has been changed
    private String domain;

    public final static String[] REQUIRED_META = new String[]{"title", "type", "image", "url" };
    
    public final static HashMap<String, String[]> BASE_TYPES = new HashMap<String, String[]>();
       static {
				BASE_TYPES.put("activity", new String[] {"activity", "sport"});
				BASE_TYPES.put("business", new String[] {"bar", "company", "cafe", "hotel", "restaurant"});
				BASE_TYPES.put("group", new String[] {"cause", "sports_league", "sports_team"});
                BASE_TYPES.put("organization", new String[] {"band", "government", "non_profit", "school", "university"});
                BASE_TYPES.put("person", new String[] {"actor", "athlete", "author", "director", "musician", "politician", "profile", "public_figure"});
                BASE_TYPES.put("place", new String[] {"city", "country", "landmark", "state_province"});
                BASE_TYPES.put("product", new String[] {"album", "book", "drink", "food", "game", "movie", "product", "song", "tv_show"});
                BASE_TYPES.put("website", new String[] {"blog", "website", "article"});
	}

   /**
    * Create an open graph representation for generating your own Open Graph object
    */
    public OpenGraph() {
        metaAttributes = new HashMap<String, String>();
        hasChanged = false;
        isImported = false;
    }

    /**
     * Fetch the open graph representation from a web site
     * @param url The address to the web page to fetch Open Graph data
     * @param ignoreSpecErrors Set this option to true if you don't wish to have an exception throw if the page does not conform to the basic 4 attributes
     * @throws java.io.IOException If a network error occurs, the HTML parser will throw an IO Exception
     * @throws java.lang.Exception A generic exception is throw if the specific page fails to conform to the basic Open Graph standard as define by the constant REQUIRED_META
     */
    public OpenGraph(String url, boolean ignoreSpecErrors) throws java.io.IOException, Exception {
        this();
        isImported = true;
        //init the attribute storage
        
        pageUrl = url;
        
        //download the (X)HTML content, but only up to the closing head tag. We do not want to waste resources parsing irrelevant content
        URL pageURL = new URL(url);
        domain = pageURL.getHost();
        URLConnection siteConnection = pageURL.openConnection();
        BufferedReader dis = new BufferedReader(new InputStreamReader(siteConnection.getInputStream()));
        String inputLine;
        StringBuffer headContents = new StringBuffer();

        //Loop through each line, looking for the closing head element
        while ((inputLine = dis.readLine()) != null) {
            headContents.append(inputLine + "\r\n");     
        }

        String headContentsStr = headContents.toString();
        HtmlCleaner cleaner = new HtmlCleaner();
        //parse the string HTML
        TagNode pageData = cleaner.clean(headContentsStr);
        //open only the meta tags
        TagNode[] metaData = pageData.getElementsByName("meta", true);
        for (TagNode metaElement : metaData) {
            if (metaElement.hasAttribute("property") && metaElement.getAttributeByName("property").startsWith("og:")) {
                metaAttributes.put(metaElement.getAttributeByName("property").replaceFirst("og:", ""), metaElement.getAttributeByName("content"));
            } else if (metaElement.hasAttribute("name") && metaElement.getAttributeByName("name").startsWith("og:")) {
                metaAttributes.put(metaElement.getAttributeByName("name").replaceFirst("og:", ""), metaElement.getAttributeByName("content"));
            }
        }

        /**
         * Check that page conforms to Open Graph protocol
         */
        if (!ignoreSpecErrors) {
            for (String req : REQUIRED_META) {
                if (!metaAttributes.containsKey(req)) {
                    throw new Exception("Does not conform to Open Graph protocol");
                }
            }
        }

        /**
         * Has conformed, now determine basic sub type.
         */
        baseType = null;
        for (String base : BASE_TYPES.keySet()) {
            String[] baseList = BASE_TYPES.get(base);
            boolean finished = false;
            for (String expandedType : baseList) {
                if (expandedType.equals(metaAttributes.get("type"))) {
                    baseType = base;
                    finished = true;
                    break;
                }
            }
            if (finished) break;
        }
        
        checkTitle(pageData);
        checkDescription(pageData);
        checkImage(pageData);
    }

	private void checkImage(final TagNode pageData) {
		if (StringUtils.isBlank(getContent("image"))) {
			metaAttributes.put("image", "http://www.getfavicon.org/?url=" + domain + "/favicon.30.png");
		}
	}

	private void checkDescription(final TagNode pageData) {
		if (StringUtils.isBlank(getContent("description"))) {
			mineDescription(pageData);
		}
	}

	private void mineDescription(final TagNode pageData) {
		searchDescriptionInMetaTags(pageData);
		
		if (StringUtils.isBlank(getContent("description"))) {
			searchDescriptionInPTags(pageData);
		}
		
		if (StringUtils.isBlank(getContent("description"))) {
			searchDescriptionInDivTags(pageData);
		}
	}

	private void searchDescriptionInDivTags(final TagNode pageData) {
		@SuppressWarnings("unchecked")
		List<TagNode> tags = pageData.getElementListByName("div", true);
		for (TagNode tag : tags) {
			metaAttributes.put("description", tag.getText().toString());
			break;
		}
	}

	private void searchDescriptionInMetaTags(final TagNode pageData) {
		@SuppressWarnings("unchecked")
		List<TagNode> tags = pageData.getElementListByName("meta", true);
		for (TagNode tag : tags) {
			if (StringUtils.equalsIgnoreCase("description", tag.getAttributeByName("name"))) {
				metaAttributes.put("description", tag.getAttributeByName("content"));
				return;
			}
		}
	}

	private void searchDescriptionInPTags(final TagNode pageData) {
		@SuppressWarnings("unchecked")
		List<TagNode> tags = pageData.getElementListByName("p", true);
		for (TagNode tag : tags) {
			metaAttributes.put("description", tag.getText().toString());
			break;
		}
	}

	private void checkTitle(final TagNode pageData) {
		if (StringUtils.isBlank(getContent("title"))) {
			mineTitle(pageData);
    	}
	}

	private void mineTitle(final TagNode pageData) {
		searchTitleTags(pageData);
		if (StringUtils.isBlank(getContent("title"))) {
			searchH1Tags(pageData);
		}
	}
	
	private void searchH1Tags(final TagNode pageData) {
		@SuppressWarnings("unchecked")
		final List<TagNode> tags = pageData.getElementListByName("h1", false);
		for (TagNode tag : tags) {
			if (StringUtils.isNotBlank(tag.getText())) {
				metaAttributes.put("title", tag.getText().toString());
				return;
			} 
		}
	}

	private void searchTitleTags(final TagNode pageData) {
		@SuppressWarnings("unchecked")
		final List<TagNode> tags = pageData.getElementListByName("title", true);
		for (TagNode tag : tags) {
			if (StringUtils.isNotBlank(tag.getText())) {
				metaAttributes.put("title", tag.getText().toString());
				return;
			} 
		}
	}

	/**
     * Get the basic type of the Open graph page as per the specification
     * @return Base type is defined by specification, null otherwise
     */
    public String getBaseType() {
        return baseType;
    }

    /**
     * Get a value of a given Open Graph property
     * @param property The Open graph property key
     * @return Returns the value of the property if defined, null otherwise
     */
    public String getContent(String property) {
        return metaAttributes.get(property);
    }

    /**
     * Get all the defined properties of the Open Graph object
     * @return An array of all currently defined properties
     */
    public String[] getProperties() {
        Set<String> valueSet = metaAttributes.keySet();
        return (String[]) valueSet.toArray();
    }

    /**
     * Get the original URL the Open Graph page was obtained from
     * @return The address to the Open Graph object page
     */
    public String getOriginalUrl() {
        return pageUrl;
    }

    /**
     * Get the HTML representation of the Open Graph data.
     * @return An array of meta elements as Strings
     */
    public String[] toHTML() {
        //allocate the array
        String[] returnHTML = new String[metaAttributes.size()];

        int index = 0; //keep track of the index to insert into
        for (String key : metaAttributes.keySet())
            returnHTML[index++] = "<meta property=\"og:" + key + "\" content=\"" + metaAttributes.get(key) + "\" />";

        //return the array
        return returnHTML;
    }

        /**
     * Get the XHTML representation of the Open Graph data.
     * @return An array of meta elements as Strings
     */
    public String[] toXHTML() {
        //allocate the array
        String[] returnHTML = new String[metaAttributes.size()];

        int index = 0; //keep track of the index to insert into
        for (String key : metaAttributes.keySet())
            returnHTML[index++] = "<meta name=\"og:" + key + "\" content=\"" + metaAttributes.get(key) + "\" />";

        //return the array
        return returnHTML;
    }

    /**
     * Set the Open Graph property to a specific value
     * @param property The og:XXXX where XXXX is the property you wish to set
     * @param content The value or contents of the property to be set
     */
    public void setProperty(String property, String content) {
        //help the user just a little bit if are confused
        if (property.startsWith("og:"))
            property = property.replaceFirst("og:", "");
        if(!hasChanged)
            hasChanged = true;

        metaAttributes.put(property, content);
    }

    /**
     * Removed a defined property
     * @param property The og:XXXX where XXXX is the property you wish to remove
     */
    public void removeProperty(String property) {
        //help the user just a little bit if are confused
        if (property.startsWith("og:"))
            property = property.replaceFirst("og:", "");
        if(!hasChanged)
            hasChanged = true;

        metaAttributes.remove(property);
    }

    /**
     * Obtain the underlying HashMap
     * @return The underlying structure as a HashMap
     */
    public HashMap<String, String> exposeTable() {
        return metaAttributes;
    }

    /**
     * Test if the Open Graph object was initially a representation of a web page
     * @return True if the object is from a web page, false otherwise
     */
    public boolean isFromWeb() {
        return isImported;
    }

    /**
     * Test if the object has been modified by setters/deleters. This is only relevant if this object initially represented a web page
     * @return True True if the object has been modified, false otherwise
     */
    public boolean hasChanged() {
        return hasChanged;
    }
    
    public static void main(String[] args) throws IOException, Exception {
		OpenGraph og = new OpenGraph("http://www.thedailybeast.com/articles/2011/08/15/iowa-straw-poll-republican-field-faces-tough-test-in-rust-belt.html", true);
		System.out.println("Title: " + og.getContent("title"));
		System.out.println("Description: " + og.getContent("description"));
		System.out.println("Image: " + og.getContent("image"));
		System.out.println("Content?: " + og.getContent("content"));
	}
    
}