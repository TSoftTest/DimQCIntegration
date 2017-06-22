package com.tsoft.dimqc.connectors.utils.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlWrapperSpan extends HtmlWrapper {

	@Override
	public String dibujar() {
		// return getSourceElement().text() + "\n";
		return getSourceElement().text();
	}

	@Override
	public String dibujarHtml() {
		if ("".equals(texto)) {
			return "<span style=\"font-size:8pt\"><br /></span>";
		} else {
			return "<span style=\"font-size:8pt\">" + limpiarTexto() + "</span>";
		}
	}

	private String limpiarTexto() {

		String textolimpio = texto;
		Pattern pattern = null;
		@SuppressWarnings("unused")
		Matcher matcher = null;

		if (ArmadoHtml.isListaNumerica(textolimpio)) {

			String regex = "^ {" + HtmlWrapperComposite.SANGRIA_SIMBOLO.length() + "}[1-9]+\\" + HtmlWrapperComposite.SIMBOLO_OL + " {"
			    + HtmlWrapperComposite.SEGUNDA_SANGRIA_SIMBOLO.length() + "}";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(textolimpio);
			textolimpio = textolimpio.replaceAll(regex, "");

		} else if (ArmadoHtml.isListaConSimbolo(texto)) {

			String regex = "^ {" + HtmlWrapperComposite.SANGRIA_SIMBOLO.length() + "}" + HtmlWrapperUl.SIMBOLO_UL + " {" + HtmlWrapperComposite.SEGUNDA_SANGRIA_SIMBOLO.length() + "}";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(textolimpio);
			textolimpio = textolimpio.replaceAll(regex, "");
		}

		return textolimpio;
	}
}
