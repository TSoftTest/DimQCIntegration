package com.tsoft.dimqc.connectors.utils.parser;

public class HtmlWrapperUl extends HtmlWrapperComposite {

	public static final String SIMBOLO_UL = "•";

	public HtmlWrapperUl() {
		super();
		addTipoTagHijo(TipoTag.LI);
	}

	@Override
	public String dibujar() {
		setSimboloLista(SIMBOLO_UL);

		return getSourceElement().ownText() + super.dibujar();
	}

	@Override
	public String dibujarHtml() {
		super.setListaNumerica(false);
		return "<ul style=\"margin-top: 0mm; margin-bottom: 0mm; list-style-type: disc; \">" + super.dibujarHtml() + "</ul>";
	}
}
