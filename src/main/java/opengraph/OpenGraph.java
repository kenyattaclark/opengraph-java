package opengraph;

import java.util.HashMap;
import java.util.Set;

public class OpenGraph {
	private String url;
    private HashMap<String, String> properties;
    private String type;
    private boolean changed;

    public OpenGraph(final String url, final HashMap<String, String> properties) {
    	this.url = url; 
    	this.properties = properties;
	}
    
    /**
     * Get the basic type of the Open graph page as per the specification
     * @return Base type is defined by specification, null otherwise
     */
    public String getType() {
        return type;
    }

    /**
     * Get the HTML representation of the Open Graph data.
     * @return An array of meta elements as Strings
     */
    public String[] toHtml() {
        //allocate the array
        String[] returnHTML = new String[properties.size()];

        int index = 0; //keep track of the index to insert into
        for (String key : properties.keySet())
            returnHTML[index++] = "<meta property=\"og:" + key + "\" content=\"" + properties.get(key) + "\" />";

        //return the array
        return returnHTML;
    }

    /**
     * Set the Open Graph property to a specific value
     * @param property The og:XXXX where XXXX is the property you wish to set
     * @param content The value or contents of the property to be set
     */
    public void setProperty(String property, String content) {
    	if (property.startsWith("og:")) {
        	property = property.replaceFirst("og:", "");
        }
    	
    	if (properties.containsKey(property)) {
    		properties.put(property, content);
        	changed = true;
        }
    }

    /**
     * Removed a defined property
     * @param property The og:XXXX where XXXX is the property you wish to remove
     */
    public void removeProperty(String property) {
        if (property.startsWith("og:")) {
        	property = property.replaceFirst("og:", "");
        }
        
        if (properties.containsKey(property)) {
        	properties.remove(property);
        	changed = true;
        }
    }
    
    /**
     * Get a value of a given Open Graph property
     * @param property The Open graph property key
     * @return Returns the value of the property if defined, null otherwise
     */
    public String getProperty(String property) {
        return properties.get(property);
    }
    
    /**
     * Get all the defined properties of the Open Graph object
     * @return An array of all currently defined properties
     */
    public String[] getPropertyList() {
        Set<String> valueSet = properties.keySet();
        return (String[]) valueSet.toArray();
    }

    /**
     * Obtain the underlying HashMap
     * @return The underlying structure as a HashMap
     */
    public HashMap<String, String> getProperties() {
        return new HashMap<String, String>(properties);
    }

    /**
     * Test if the object has been modified by setters/deleters. This is only relevant if this object initially represented a web page
     * @return True True if the object has been modified, false otherwise
     */
    public boolean isChanged() {
        return changed;
    }
    
    public String getUrl() {
		return url;
	}
}