package com.tsoft.dimqc.connectors.utils.parser;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;

public abstract class HtmlWrapperComposite implements HtmlElement {

	protected List<HtmlElement> hijos = new ArrayList<HtmlElement>();
	protected Element sourceElement;
	protected static String simboloLista = "";
	public static final String SANGRIA_SIMBOLO = "   ";
	public static final String SEGUNDA_SANGRIA_SIMBOLO = "  ";
	public static final String SIMBOLO_OL = ".";

	@Override
	public String dibujar() {
		StringBuilder sb = new StringBuilder();

		for (HtmlElement e : getHijos()) {
			sb.append(e.dibujar());
		}

		if (getHijos().size() == 1 && getHijos().get(0) instanceof HtmlWrapperBr) {

			String textoAux = getSourceElement().outerHtml().replaceAll("<body>", "");
			textoAux = textoAux.trim();
			textoAux = textoAux.replaceAll("</body>", "");
			textoAux = textoAux.trim();
			String[] texto = textoAux.split("<br />");

			for (int i = 0; i < texto.length - 1; i++) {
				sb.append(texto[i]);
			}
			sb.append(texto[texto.length - 1]);
		} else if (getHijos().isEmpty()) {
			sb.append(getSourceElement().text());
		}
		return sb.toString();
	}

	public List<HtmlElement> getHijos() {
		return hijos;
	}

	public void addHijo(HtmlElement ele) {
		hijos.add(ele);
	}

	@Override
	public Element getSourceElement() {
		return sourceElement;
	}

	@Override
	public void setSourceElement(Element element) {
		this.sourceElement = element;
	}

	public static String getSimboloLista() {

		String simbolo = simboloLista;
		// si es un nro => incremento en uno por cada valor devuelto
		try {
			int nro = Integer.parseInt(simboloLista.trim());
			nro++;
			simboloLista = String.valueOf(nro);
			simbolo = SANGRIA_SIMBOLO + simbolo + SIMBOLO_OL + SEGUNDA_SANGRIA_SIMBOLO;

		} catch (NumberFormatException e) {
			// No es una lista numerada, sino que usa simbolo.
			simbolo = SANGRIA_SIMBOLO + simbolo + SEGUNDA_SANGRIA_SIMBOLO;
		}

		return simbolo;
	}

	public static void setSimboloLista(String simbolo) {
		simboloLista = simbolo;
	}

	// desde aca esta lo d Tag....

	// protected List<TagElement> hijosTagElement = new ArrayList<TagElement>();
	protected List<TipoTag> tipoTagHijos = new ArrayList<TipoTag>();
	protected String texto = "";
	protected static boolean listaNumerica = false;

	@Override
	public String dibujarHtml() {
		StringBuilder sb = new StringBuilder();

		for (TagElement e : getHijos()) {
			sb.append(e.dibujarHtml());
		}

		return sb.toString();
	}

	// public List<TagElement> getHijosTagElement() {
	// return hijosTagElement;
	// }
	//
	// public void addHijoTagElement(TagElement ele){
	// hijosTagElement.add(ele);
	// }

	public List<TipoTag> getTipoTagHijos() {
		return tipoTagHijos;
	}

	public void addTipoTagHijo(TipoTag ele) {
		tipoTagHijos.add(ele);
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public static boolean isListaNumerica() {
		return listaNumerica;
	}

	public static void setListaNumerica(boolean listaNumerica) {
		HtmlWrapperComposite.listaNumerica = listaNumerica;
	}
}
