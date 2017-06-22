package ar.com.tssa.serena.connectors.utils.parser;



public class HtmlWrapperFont extends HtmlWrapperComposite {

	public HtmlWrapperFont(){
		super();
		addTipoTagHijo(TipoTag.SPAN);
	}
	
	@Override
	public String dibujar() {
		//return getSourceElement().ownText() + super.dibujar();
		if (getSourceElement().nextElementSibling() != null){
			if (getSourceElement().nextElementSibling().text() != null && !"".equals(getSourceElement().nextElementSibling().text())){
				
				if ((getSourceElement().nextElementSibling().text().charAt(0) >= 'a' && getSourceElement().nextElementSibling().text().charAt(0) <= 'z')
					|| (getSourceElement().nextElementSibling().text().charAt(0) >= 'A' && getSourceElement().nextElementSibling().text().charAt(0) <= 'Z')){
					return getSourceElement().ownText() + super.dibujar() + " ";
				}
			}
		}
		
		return getSourceElement().ownText() + super.dibujar();
		
	}
	
	@Override
	public String dibujarHtml() {
		return "<font face=\"Arial\">" + super.dibujarHtml() + "</font>";
	}
}
