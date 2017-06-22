package com.tsoft.dimqc.connectors.utils.parser;

public class HtmlWrapperP extends HtmlWrapperComposite {

	@Override
	public String dibujar() {
		if (getHijos().isEmpty()) {
			return "\n";
		} else {
			return super.dibujar() + "\n";
		}
	}
}
