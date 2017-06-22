package com.tsoft.dimqc.connectors.alm;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "totalResults", "entities" })
@XmlRootElement(name = "Entities")
public class Entities {
	@XmlAttribute(name = "TotalResults", required = true)
	protected int totalResults;

	@XmlElement(name = "Entity")
	protected List<Entity> entities;

	public int getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(int totalResults) {
		this.totalResults = totalResults;
	}

	public Entities() {
	}

	public List<Entity> getEntities() {
		if (entities == null) {
			entities = new ArrayList<Entity>();
		}
		return this.entities;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

}
