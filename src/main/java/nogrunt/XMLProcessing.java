package nogrunt;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLProcessing {
	
	public String getFromXMLFile(String filename, String query){
        try {
            File inputFile = new File(filename);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputFile);

            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();

            //query = "player~name=Rohit~team=India~proficiency";

            if(query.contains("~")){
                String tagName = query.substring(0, query.indexOf('~'));;
                query = query.substring(query.indexOf('~')+1);
                if(root.getNodeType() == Node.ELEMENT_NODE){
                    if(root.getNodeName().equals(tagName)){
                        return processXML(root, query);
                    }
                    else {
                        return null;
                    }
                }
            } else{
                return root.getTextContent();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String processXML(Element element, String query){
        NodeList childNodes = element.getChildNodes();

        if(query.contains("~")){
            String temp = query.substring(0, query.indexOf('~'));
            String tagName = temp;
            String tagValue = "";
            if(temp.contains("=")){
                tagName = temp.substring(0, temp.indexOf('='));
                tagValue = temp.substring(temp.indexOf('=')+1);
            }
            query = query.substring(query.indexOf('~')+1);

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE){
                    Element currentElement = (Element) node;

                    if(currentElement.getNodeName().equals(tagName)){

                        if(currentElement.hasAttributes()){

                            NamedNodeMap attributes = currentElement.getAttributes();

                            for (int j = 0; j < attributes.getLength(); j++) {
                                Node attribute = attributes.item(j);
                                if(!tagValue.isEmpty() && attribute.getNodeValue().equals(tagValue)){
                                    return processXML(currentElement, query);
                                }
                            }
                        } else {
                            return processXML(currentElement, query);
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if(childNode.getNodeType() == Node.ELEMENT_NODE){
                    String nodeName = childNode.getNodeName();
                    if(query.equals(nodeName)){
                        return childNode.getTextContent();
                    }
                }
            }
        }
        return null;
    }

}
