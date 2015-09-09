package com.spectralmind.sf4android.definitions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.graphics.Color;
import android.graphics.PointF;

import com.google.common.collect.Lists;

public class ClusterDefinitionLoader {

	private final XPathExpression clustersExpression;
	private final XPathExpression nameExpression;
	private final XPathExpression subExpression;
	private final XPathExpression centerXExpression;
	private final XPathExpression centerYExpression;
	private final XPathExpression colorExpression;

	public ClusterDefinitionLoader() {
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			clustersExpression = xPath.compile("//clusters/cluster");
			nameExpression = xPath.compile("./name/text()");
			subExpression = xPath.compile("./sub/text()");
			centerXExpression = xPath.compile("./x/text()");
			centerYExpression = xPath.compile("./y/text()");
			colorExpression = xPath.compile("./color/text()");
		}
		catch(XPathException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
	}

	public List<ClusterDefinition> loadDefinitions(InputStream configurationStream) {
		try {
			List<ClusterDefinition> definitions = Lists.newArrayList();
			for (Node clusterNode : getClusterNodes(configurationStream)) {
				definitions.add(createDefinitionFromNode(clusterNode));
			}			
			return definitions;
		}
		catch(XPathException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
		catch(SAXException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
		catch(ParserConfigurationException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
		catch(IOException e) {
			throw new DefinitionLoaderException("Could not load configuration", e);
		}
	}

	public List<ClusterAttributeDefinition> loadAttributeDefinitions(InputStream configurationStream) {
		try {
			List<ClusterAttributeDefinition> definitions = Lists.newArrayList();
			for(Node genreNode : getClusterNodes(configurationStream)) {
				definitions.add(createAttributeDefinitionFromNode(genreNode));
			}			
			return definitions;
		}
		catch(XPathException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
		catch(SAXException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
		catch(ParserConfigurationException e) {
			throw new DefinitionLoaderException("Could not parse configuration", e);
		}
		catch(IOException e) {
			throw new DefinitionLoaderException("Could not load configuration", e);
		}
	}
	
	
	private List<Node> getClusterNodes(InputStream configurationStream) throws ParserConfigurationException,
			SAXException, IOException, XPathException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(configurationStream);
		doc.getDocumentElement().normalize();
		NodeList clusterNodes = (NodeList) clustersExpression.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
		List<Node> nodes = Lists.newArrayList();
		for(int i = 0; i < clusterNodes.getLength(); ++i) {
			nodes.add(clusterNodes.item(i));
		}
		return nodes;
	}

	private ClusterDefinition createDefinitionFromNode(Node genreNode) throws NumberFormatException, XPathException {
		return new ClusterDefinition(nameExpression.evaluate(genreNode), parseSub(genreNode));
	}
	
	private ClusterAttributeDefinition createAttributeDefinitionFromNode(Node genreNode) throws NumberFormatException, XPathException {
		return new ClusterAttributeDefinition(parseCenter(genreNode), parseColor(genreNode));
	}

	private PointF parseCenter(Node genreNode) throws NumberFormatException, XPathException {
		return new PointF(Integer.parseInt(centerXExpression.evaluate(genreNode)), Integer.parseInt(centerYExpression
				.evaluate(genreNode)));
	}

	private List<String> parseSub(Node genreNode) throws XPathException {
		List<String> l = Lists.newArrayList(subExpression.evaluate(genreNode).split(",", -1));
		// return null if it would otherwise be an array with one element, the empty string 
		if (l.size() == 1 && l.get(0).equals(""))
			return null;
		else
			return Lists.newArrayList(subExpression.evaluate(genreNode).split(",", -1));
	}

	private Integer parseColor(Node genreNode) throws XPathException {
		return Integer.valueOf(Color.parseColor("#" + colorExpression.evaluate(genreNode)));
	}
}
