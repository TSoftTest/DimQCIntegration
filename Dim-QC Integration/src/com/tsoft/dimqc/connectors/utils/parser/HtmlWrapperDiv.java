package com.tsoft.dimqc.connectors.utils.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlWrapperDiv extends HtmlWrapperComposite {

	public static final String TABULACION = "      ";

	public HtmlWrapperDiv() {
		super();
		addTipoTagHijo(TipoTag.FONT);
	}

	public String getSangria() {

		String sangria = "";

		if (getSourceElement() != null) {
			String style = getSourceElement().attr("style");

			if (style != null && !"".equals(style)) {
				String[] styles = style.split(";");

				boolean encontrado = false;
				int i = 0;
				while (!encontrado) {

					String[] valoresInterno = styles[i].split(":");

					if ("margin-left".equals(valoresInterno[0])) {
						if (valoresInterno[1].contains("mm")) {
							String valorNro = valoresInterno[1].substring(0, valoresInterno[1].indexOf("mm"));
							int nro = 0;

							try {
								nro = Integer.parseInt(valorNro);
							} catch (NumberFormatException e) {

							}

							if (nro > 0) {
								sangria = TABULACION;
							}
						}

						encontrado = true;
					}

					i++;
				}
			}
		}

		return sangria;
	}

	@Override
	public String dibujar() {
		if (getHijos().isEmpty()) {
			return getSangria() + getSourceElement().ownText() + "\n";
		} else {
			return getSangria() + getSourceElement().ownText() + super.dibujar() + "\n";
		}

	}

	@Override
	public String dibujarHtml() {
		StringBuilder sb = new StringBuilder("");

		if (tieneTabulacion()) {
			sb.append("<div align=\"left\" style=\"margin-left:13mm; margin-right:0mm; text-indent:0mm; margin-top:0mm; margin-bottom:0mm; \" >" + super.dibujarHtml() + "</div>");
		} else {
			sb.append("<div align=\"left\">" + super.dibujarHtml() + "</div>");
		}

		return sb.toString();
	}

	private boolean tieneTabulacion() {

		String regex = "^ {" + HtmlWrapperDiv.TABULACION.length() + "}.*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(texto);
		return matcher.matches();

	}

}