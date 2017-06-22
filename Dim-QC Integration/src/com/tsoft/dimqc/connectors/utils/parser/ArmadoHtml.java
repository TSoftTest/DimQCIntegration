package com.tsoft.dimqc.connectors.utils.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmadoHtml {

	private String lineaTexto = "";

	public String armar(String texto) {

		StringBuilder sb = new StringBuilder("");
		Map<Integer, Tag> tags = obtenerTags(texto);

		sb.append("<html>");
		sb.append((new HtmlWrapperHead()).dibujarHtml());

		sb.append("<body>");

		for (Tag tag : tags.values()) {
			HtmlElement wrapResult = wrap(tag.getTipoTag(), tag.getCantidad());
			int i = 1;

			if (TipoTag.DIV.getValue().equals(tag.getTipoTag().getValue())) {
				wrapResult.setTexto(tag.getTextos().get(i));
			}

			setearTextos(wrapResult, tag, i);

			sb.append(wrapResult.dibujarHtml());
		}

		sb.append("</body>");
		sb.append("</html>");

		return sb.toString();

	}

	private HtmlElement wrap(TipoTag element, int cantHijos) {

		HtmlElement w = new HtmlWrapperFactory().build(element);

		if (w instanceof HtmlWrapperComposite) {
			HtmlWrapperComposite comp = (HtmlWrapperComposite) w;
			for (TipoTag ele : comp.getTipoTagHijos()) {
				for (int i = 0; i < cantHijos; i++) {
					comp.addHijo(wrap(ele, 1)); // puse 1 xq tal cual esta armado solo
																			// tiene un hijo cada tag
				}
			}
		}

		return w;
	}

	private void setearTextos(HtmlElement wrapResult, Tag tag, int i) {

		if (wrapResult instanceof HtmlWrapperComposite) {
			HtmlWrapperComposite comp = (HtmlWrapperComposite) wrapResult;
			for (HtmlElement ele : comp.getHijos()) {
				ele.setTexto(tag.getTextos().get(i));
				setearTextos(ele, tag, i);
				i++;
			}

		}
	}

	public Map<Integer, Tag> obtenerTags(String texto) {

		Scanner scanner = new Scanner(texto);
		Map<Integer, Tag> tags = new HashMap<Integer, Tag>();

		String linea = "";
		boolean leer = true;
		int nroLinea = 1;
		while (scanner.hasNextLine() || !leer) {

			if (leer || linea == null) {
				linea = scanner.nextLine();
			} else {
				linea = lineaTexto;
			}

			TipoTag tipoTag = getTag(linea);
			Tag tag = new Tag();
			tag.setTipoTag(tipoTag);
			tag.setCantidad(1);
			tag.addTextos(1, linea);

			if (TipoTag.DIV.getValue().equals(tipoTag.getValue())) {
				tags.put(nroLinea, tag);
				leer = true;
			} else if (TipoTag.OL.getValue().equals(tipoTag.getValue()) || TipoTag.UL.getValue().equals(tipoTag.getValue())) { // este
																																																												 // y
																																																												 // el
																																																												 // siguiente
																																																												 // pueden
																																																												 // ir
																																																												 // juntos
				obtenerTagHijos(scanner, tag, 1);
				tags.put(nroLinea, tag);
				leer = false;
			}
			nroLinea++;
		}

		scanner.close();

		// Por defecto los ordena de menor a mayor. En este caso, se van a ordenar
		// por numero de linea de menor a mayor.
		Map<Integer, Tag> tagsOrdenados = new TreeMap<Integer, Tag>(tags);

		return tagsOrdenados;
	}

	private void obtenerTagHijos(Scanner scanner, Tag tag, int cantidad) {

		String linea = scanner.nextLine();

		TipoTag tipoTag = getTag(linea);

		if (tipoTag.getValue().equals(tag.getTipoTag().getValue())) {
			tag.setCantidad(cantidad + 1);
			tag.addTextos(cantidad + 1, linea);
			if (scanner.hasNextLine()) {
				obtenerTagHijos(scanner, tag, cantidad + 1);
			}
		} else {
			lineaTexto = linea;
		}
	}

	public TipoTag getTag(String linea) {
		/*
		 * Esto solo va a devolver si es Div, ol, ul.
		 */

		if (isListaNumerica(linea)) {
			return TipoTag.OL;
		} else if (isListaConSimbolo(linea)) {
			return TipoTag.UL;
		} else {
			return TipoTag.DIV;
		}
	}

	public static boolean isListaNumerica(String linea) {
		// String regex = "^ {3}[1-9]+\\. {2}.*";

		String regex = "^ {" + HtmlWrapperComposite.SANGRIA_SIMBOLO.length() + "}[1-9]+\\" + HtmlWrapperComposite.SIMBOLO_OL + " {"
		    + HtmlWrapperComposite.SEGUNDA_SANGRIA_SIMBOLO.length() + "}.*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(linea);
		return matcher.matches();
	}

	public static boolean isListaConSimbolo(String linea) {
		String regex = "^ {" + HtmlWrapperComposite.SANGRIA_SIMBOLO.length() + "}" + HtmlWrapperUl.SIMBOLO_UL + " {" + HtmlWrapperComposite.SEGUNDA_SANGRIA_SIMBOLO.length() + "}.*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(linea);
		return matcher.matches();
	}

	public class Tag {
		private TipoTag tipoTag;
		private int cantidad;
		private Map<Integer, String> textos = new HashMap<Integer, String>();

		public Tag() {
			super();
		}

		public TipoTag getTipoTag() {
			return tipoTag;
		}

		public void setTipoTag(TipoTag tipoTag) {
			this.tipoTag = tipoTag;
		}

		public int getCantidad() {
			return cantidad;
		}

		public void setCantidad(int cantidad) {
			this.cantidad = cantidad;
		}

		public Map<Integer, String> getTextos() {
			return textos;
		}

		public void addTextos(Integer key, String valor) {
			textos.put(key, valor);
		}
	}
}
