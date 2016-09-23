/*
 * Parameter.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.ribeiro.android.gso.dataclasses;

public class Parameter {
    private String name;
    private String value;

    /**
     * Erstellt ein neues Parameter object
     *
     * @param name  String Bezeichnung f�r den Parameter
     * @param value String Wert f�r den Parameter
     */
    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Liefert den Namen des Parameters
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Liefert den Wert des Parameters
     *
     * @return
     */
    public String getValue() {
        return this.value;
    }
}
