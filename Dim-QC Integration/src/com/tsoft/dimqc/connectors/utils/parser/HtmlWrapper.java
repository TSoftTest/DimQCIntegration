package com.tsoft.dimqc.connectors.utils.parser;

import org.jsoup.nodes.Element;

public abstract class HtmlWrapper implements HtmlElement {

	protected Element sourceElement;
	protected String texto = "";

	@Override
	public Element getSourceElement() {
		return sourceElement;
	}

	@Override
	public void setSourceElement(Element element) {
		this.sourceElement = element;
	}

	@Override
	public String dibujarHtml() {
		return "";
	}

	@Override
	public String getTexto() {
		return texto;
	}

	@Override
	public void setTexto(String texto) {
		this.texto = texto;
	}
}
