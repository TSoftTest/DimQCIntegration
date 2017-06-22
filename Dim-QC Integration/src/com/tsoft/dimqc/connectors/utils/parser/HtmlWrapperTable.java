package com.tsoft.dimqc.connectors.utils.parser;

public class HtmlWrapperTable extends HtmlWrapperComposite {

	@Override
	public String dibujar() {
		// return getSourceElement().ownText() + "\n" + super.dibujar();

		/*
		 * Esto es asi porque no se pidio dibujar la tabla. Por ende, no se le
		 * asigna un comportamiento en particular
		 */
		return "";
	}
}
