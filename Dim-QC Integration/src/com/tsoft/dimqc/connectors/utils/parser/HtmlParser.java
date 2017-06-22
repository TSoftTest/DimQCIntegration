package com.tsoft.dimqc.connectors.utils.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlParser {

	public HtmlElement parse(Document doc) /* throws ParseException */{

		// Obtengo el body
		Element body = doc.body();

		HtmlElement wrapResult = wrap(body);

		// String res = wrapResult.dibujar();
		//
		// System.out.println(res);

		return wrapResult;

	}

	private HtmlElement wrap(Element element) {
		HtmlElement w = new HtmlWrapperFactory().build(element);

		if (w instanceof HtmlWrapperComposite) {
			HtmlWrapperComposite comp = (HtmlWrapperComposite) w;

			for (Element ele : comp.getSourceElement().children()) {
				comp.addHijo(wrap(ele));
			}
		}

		return w;
	}

}
