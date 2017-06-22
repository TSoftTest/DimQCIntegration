package com.tsoft.dimqc.connectors.dto;

import java.util.ArrayList;
import java.util.List;

public class CampoMultiplesDTO {

	private String atributoDimPrincipal = "";
	private List<String> atributosCombinacion = new ArrayList<String>();

	public CampoMultiplesDTO() {
	}

	public CampoMultiplesDTO(String atributoDimPrincipal, List<String> atributosCombinacion) {
		this.atributoDimPrincipal = atributoDimPrincipal;
		this.atributosCombinacion = atributosCombinacion;
	}

	public String getAtributoDimPrincipal() {
		return atributoDimPrincipal;
	}

	public void setAtributoDimPrincipal(String atributoDimPrincipal) {
		this.atributoDimPrincipal = atributoDimPrincipal;
	}

	public List<String> getAtributosCombinacion() {
		return atributosCombinacion;
	}

	public void addAtributosCombinacion(String atributoCombinacion) {
		if (this.atributosCombinacion == null) {
			this.atributosCombinacion = new ArrayList<String>();
		}
		this.atributosCombinacion.add(atributoCombinacion);
	}
}
