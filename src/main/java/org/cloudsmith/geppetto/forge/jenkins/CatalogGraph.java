/*******************************************************************
 * Copyright (c) 2013, Cloudsmith Inc.
 * The code, documentation and other materials contained herein
 * are the sole and exclusive property of Cloudsmith Inc. and may
 * not be disclosed, used, modified, copied or distributed without
 * prior written consent or license from Cloudsmith Inc.
 ******************************************************************/
package org.cloudsmith.geppetto.forge.jenkins;

import java.io.Serializable;

/**
 * The CatalogGraph represents the result of deploying a host. It contains
 * a base-64 encoded string representing the bytes of an SVG image which in turn
 * represents the graph.
 */
public class CatalogGraph implements Serializable {
	private static final long serialVersionUID = 5189972109353893244L;

	private String nodeName;

	private String instanceID;

	private String catalogGraph;

	/**
	 * @return the catalogGraph
	 */
	public String getCatalogGraph() {
		return catalogGraph;
	}

	public String getInstanceID() {
		return instanceID;
	}

	public String getNodeName() {
		return nodeName;
	}

	/**
	 * @param catalogGraph
	 *            the catalogGraph to set
	 */
	public void setCatalogGraph(String catalogGraph) {
		this.catalogGraph = catalogGraph;
	}

	public void setInstanceID(String instanceID) {
		this.instanceID = instanceID;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
}
