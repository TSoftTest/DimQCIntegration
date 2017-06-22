package ar.com.tssa.serena.connectors.utils.parser;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;


public class HtmlWrapperFactory {

	private Map<String, Class<? extends HtmlElement>> clazzes = new HashMap<String, Class<? extends HtmlElement>>();
	private Map<TipoTag, Class<? extends HtmlElement>> clasesTagElement = new HashMap<TipoTag, Class<? extends HtmlElement>>();
	private Logger logger = Logger.getRootLogger();
	
	public HtmlWrapperFactory() {

		clazzes.put("html", HtmlWrapperHtml.class);
		clazzes.put("body", HtmlWrapperBody.class);
		clazzes.put("tbody", HtmlWrapperTBody.class);
		clazzes.put("table", HtmlWrapperTable.class);
		clazzes.put("tr", HtmlWrapperTr.class);
		clazzes.put("td", HtmlWrapperTd.class);
		clazzes.put("div", HtmlWrapperDiv.class);
		clazzes.put("font", HtmlWrapperFont.class);
		clazzes.put("span", HtmlWrapperSpan.class);
		clazzes.put("colgroup", HtmlWrapperColgroup.class);
		clazzes.put("col", HtmlWrapperCol.class);
		clazzes.put("ol", HtmlWrapperOl.class);
		clazzes.put("li", HtmlWrapperLi.class);
		clazzes.put("ul", HtmlWrapperUl.class);
		clazzes.put("br", HtmlWrapperBr.class);
		clazzes.put("p", HtmlWrapperP.class);
		
		clasesTagElement.put(TipoTag.HTML, HtmlWrapperHtml.class);
		clasesTagElement.put(TipoTag.HEAD, HtmlWrapperHead.class);
		clasesTagElement.put(TipoTag.BODY, HtmlWrapperBody.class);
		clasesTagElement.put(TipoTag.DIV, HtmlWrapperDiv.class);
		clasesTagElement.put(TipoTag.FONT, HtmlWrapperFont.class);
		clasesTagElement.put(TipoTag.SPAN, HtmlWrapperSpan.class);
		clasesTagElement.put(TipoTag.OL, HtmlWrapperOl.class);
		clasesTagElement.put(TipoTag.LI, HtmlWrapperLi.class);
		clasesTagElement.put(TipoTag.UL, HtmlWrapperUl.class);
	}
	
	
	public HtmlElement build(Element element){

		Class<? extends HtmlElement> clazz = clazzes.get(element.tag().getName()); //clazzes.get(TipoTag.valueOf(element.tag().getName())); //clazzes.get(element.tag().getName());
		
		HtmlElement c = null;
		try {
			clazz.getDeclaredConstructors()[0].setAccessible(true);
			
			c = (HtmlElement) clazz.getDeclaredConstructors()[0].newInstance();
			
			c.setSourceElement(element);
			
		} catch (Exception e){
			logger.error("No se encontro el tag:" + element.tag().getName());
			e.printStackTrace();
		}
		return c;
	}
	
	public HtmlElement build(TipoTag element){

		Class<? extends HtmlElement> clazz = clasesTagElement.get(element);
		
		HtmlElement c = null;
		try {
			clazz.getDeclaredConstructors()[0].setAccessible(true);
			
			c = (HtmlElement) clazz.getDeclaredConstructors()[0].newInstance();
			
		} catch (Exception e){
			logger.error("No se encontro clase para el tag:" + element.getValue());
			e.printStackTrace();
		}
		return c;
	}
}
