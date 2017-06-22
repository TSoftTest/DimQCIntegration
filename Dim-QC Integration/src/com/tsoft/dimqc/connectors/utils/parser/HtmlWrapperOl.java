package com.tsoft.dimqc.connectors.utils.parser;

public class HtmlWrapperOl extends HtmlWrapperComposite {

	public HtmlWrapperOl() {
		super();
		addTipoTagHijo(TipoTag.LI);
	}

	@Override
	public String dibujar() {
		setSimboloLista("1");

		return getSourceElement().ownText() + super.dibujar();
	}

	@Override
	public String dibujarHtml() {
		super.setListaNumerica(true);
		return "<ol style=\"margin-top: 0mm; margin-bottom: 0mm;  \">" + super.dibujarHtml() + "</ol>";
	}
}
