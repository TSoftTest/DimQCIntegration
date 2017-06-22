package com.tsoft.dimqc.connectors.utils.parser;

import org.jsoup.nodes.Element;

/**
 * Define el comportamiento de los elementos html
 * 
 * @author ediaz
 *
 */
public interface HtmlElement extends TagElement {

	/**
	 * Dibuja un texto plano a partir de elementos html.
	 *
	 */
	String dibujar();

	Element getSourceElement();

	void setSourceElement(Element element);

}
