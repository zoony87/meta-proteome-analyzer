package de.mpa.client.model.dbsearch;

import java.io.Serializable;

public enum SearchEngineType implements Serializable {
	MSGF("MS-GF+"),
	XTANDEM("X!Tandem"), 
	OMSSA("OMSSA"); 

	SearchEngineType(final String searchEngineName){
		this.searchEngineName = searchEngineName;
	}
	
	private final String searchEngineName;
	
	@Override
	public String toString() {
		return searchEngineName;
	}
}
