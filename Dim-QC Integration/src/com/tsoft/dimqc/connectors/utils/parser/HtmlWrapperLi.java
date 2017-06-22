package com.tsoft.dimqc.connectors.utils.parser;

public class HtmlWrapperLi extends HtmlWrapperComposite {

	public HtmlWrapperLi() {
		super();
		addTipoTagHijo(TipoTag.FONT);
	}

	@Override
	public String dibujar() {

		// String res = getSimboloLista() + getSourceElement().ownText() +
		// super.dibujar();
		//
		// //res += "\n";
		//
		// return res;
		return getSimboloLista() + getSourceElement().ownText() + super.dibujar() + "\n";
	}

	@Override
	public String dibujarHtml() {

		if (super.isListaNumerica()) {
			return "<li style=\"font-family: arial;  color: #010101;  \">" + super.dibujarHtml() + "</li>";
		} else {
			return "<li style=\"font-size: 10pt;  color: #010101;  \">" + super.dibujarHtml() + "</li>";
		}
	}
}
