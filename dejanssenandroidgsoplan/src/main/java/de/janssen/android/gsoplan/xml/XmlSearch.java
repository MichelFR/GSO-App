package de.janssen.android.gsoplan.xml;

import de.janssen.android.gsoplan.ArrayOperations;
import de.janssen.android.gsoplan.dataclasses.Parameter;

public class XmlSearch {
    private Xml tagCrawlerResult;
    private Xml[] tagCrawlerResultArray;

    /**
     * @param xmlResource Xml in dem gesucht werden soll
     * @param xmlSearch   Xml der den zu suchenden Tag-Typen enth�lt
     * @return Xml mit Suchergebnis
     * @author Tobias Janssen Sucht in dem angegebenen XmlTag abw�rts nach dem
     * gew�nschten tagType
     */
    public Xml tagCrawlerFindFirstOf(Xml xmlResource, Xml xmlSearch) {
        return tagCrawlerFindFirstOf(xmlResource, xmlSearch, false);
    }

    /**
     * @param xmlResource Xml in dem gesucht werden soll
     * @param xmlSearch   Xml der den zu suchenden Tag-Typen enth�lt
     * @return Xml mit Suchergebnis
     * @author Tobias Janssen Sucht in dem angegebenen XmlTag abw�rts nach dem
     * gew�nschten tagType
     */
    public Xml tagCrawlerFindFirstOf(Xml xmlResource, Xml xmlSearch, Boolean upwards) {
        // pr�fen, ob das resultArray bereits initialisiert wurde:
        if (tagCrawlerResult != null) {
            // resultArray initialisieren
            return tagCrawlerResult;
        }

        // pr�fen, ob der aktuelle HtmlTag dem zu suchenden Tag entspricht
        if (compareTypes(xmlResource, xmlSearch) && compareContent(xmlResource, xmlSearch)
                && compareParameter(xmlResource, xmlSearch)) {
            // fund
            return xmlResource;
        }
        if (!upwards) {
            // pr�fen, ob es noch untergeordnete Tags gibt:
            if (xmlResource.getChildTags().length > 0) {
                // alle childTags per Rekursion pr�fen:
                for (int i = 0; i < xmlResource.getChildTags().length && tagCrawlerResult == null; i++) {
                    tagCrawlerResult = tagCrawlerFindFirstOf(xmlResource.getChildTagAtIndex(i), xmlSearch, upwards);
                }
            }
        } else {
            if (xmlResource.getParentTag() != null) {
                // den parentTags per Rekursion pr�fen:
                tagCrawlerResult = tagCrawlerFindFirstOf(xmlResource.getParentTag(), xmlSearch, upwards);
            }
        }
        return tagCrawlerResult;
    }

    /**
     * Vergleicht die beiden Xml Objekte anhand des Typs.
     * <p/>
     * gibt ein true aus, wenn das suchobjekt einen ungesetzen typ hat,
     * <p/>
     * oder eine �bereinstimming vorhanden ist
     *
     * @param xmlResource Xml 1
     * @param xmlSearch   Xml 2
     * @return true, wenn gleicher Typ
     * @author Tobias Janssen
     */
    private Boolean compareTypes(Xml xmlResource, Xml xmlSearch) {
        // pr�fen, ob der aktuelle xmlResourceTag dem zu suchenden Tag
        // entspricht
        if (xmlSearch.getType() == null)
            // nein, soll nicht gepr�pft werden, daher wird ein match ausgegeben
            return true;
        else if (xmlSearch.getType().equalsIgnoreCase(Xml.UNSET))
            // nein, soll nicht gepr�pft werden, daher wird ein match ausgegeben
            return true;
        else if (xmlResource.getType().equalsIgnoreCase(xmlSearch.getType()))
            return true;

        return false;
    }

    /**
     * Vergleicht die beiden Xml Objekte anhand des Contents.
     * <p/>
     * gibt ein true aus, wenn das suchobjekt einen ungesetzen content hat,
     * <p/>
     * oder eine �bereinstimming vorhanden ist
     *
     * @param xmlResource
     * @param xmlSearch
     * @return
     * @author Tobias Janssen
     */
    private Boolean compareContent(Xml xmlResource, Xml xmlSearch) {
        // pr�fen, ob der der DataContent gepr�ft werden soll
        if (xmlSearch.getDataContent() == null)
            // nein, soll nicht gepr�pft werden, daher wird ein match ausgegeben
            return true;
        else if (xmlSearch.getDataContent().equalsIgnoreCase(""))
            // nein, soll nicht gepr�pft werden, daher wird ein match ausgegeben
            return true;
        else if (xmlResource.getDataContent() != null
                && xmlResource.getDataContent().contains(xmlSearch.getDataContent()))
            return true;

        return false;
    }

    /**
     * Vergleicht die beiden Xml Objekte anhand des Parameters.
     * <p/>
     * gibt ein true aus, wenn das suchobjekt einen ungesetzen content hat,
     * <p/>
     * oder eine �bereinstimming vorhanden ist
     *
     * @param xmlResource
     * @param xmlSearch
     * @return
     * @author Tobias Janssen
     */
    private Boolean compareParameter(Xml xmlResource, Xml xmlSearch) {
        // pr�fen, ob der der DataContent gepr�ft werden soll
        if (xmlSearch.getParameters() == null)
            // nein, soll nicht gepr�pft werden, daher wird ein match ausgegeben
            return true;
        else if (xmlSearch.getParameters().length == 0)
            // nein, soll nicht gepr�pft werden, daher wird ein match ausgegeben
            return true;
        else {

            if (xmlResource.getParameters() != null && xmlSearch.getParameterAtIndex(1) != null) {
                Parameter parameter = xmlSearch.getParameterAtIndex(1);
                // alle vorhandenen parameter abrufen...
                for (int i = 0; i < xmlResource.getParameters().length; i++) {
                    // ...und �berpr�fen, ob der Parametername mit Value mit den
                    // Suchanforderungen �bereinstimmt:
                    Parameter para = xmlResource.getParameterAtIndex(i);
                    if (para.getName().equalsIgnoreCase(parameter.getName())) {
                        if (parameter.getValue() != null && para.getValue().equalsIgnoreCase(parameter.getValue())) {
                            // Fund
                            return true;
                        } else if (parameter.getValue() == null) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * @param xmlResource Xml in dem gesucht werden soll
     * @param tagType     String der den zu suchenden Tag-Typen enth�lt
     * @param parameter   Parameter, der den zu suchenden Parameter enth�lt
     * @return Xml mit Suchergebnis
     * @author Tobias Janssen Sucht in dem angegebenen XmlTag abw�rts nach dem
     * gew�nschten tagType, parameter & parameter value
     */
    public Xml tagCrawlerFindFirstOf(Xml xmlResource, String tagType, Parameter parameter) {
        // pr�fen, ob das resultArray bereits initialisiert wurde:
        if (tagCrawlerResult != null) {
            // resultArray initialisieren
            return tagCrawlerResult;
        }

        // pr�fen, ob der aktuelle HtmlTag dem zu suchenden Tag entspricht und
        // ob dieser �berhaupt Parameter enth�lt
        if (xmlResource.getType().equalsIgnoreCase(tagType) && xmlResource.getParameters().length > 0) {
            // alle vorhandenen parameter abrufen...
            for (int i = 0; i < xmlResource.getParameters().length; i++) {
                // ...und �berpr�fen, ob der Parametername mit Value mit den
                // Suchanforderungen �bereinstimmt:
                Parameter para = xmlResource.getParameterAtIndex(i);
                if (para.getName().equalsIgnoreCase(parameter.getName())) {
                    if (parameter.getValue() != null && para.getValue().equalsIgnoreCase(parameter.getValue())) {
                        // Fund
                        return xmlResource;
                    } else if (parameter.getValue() == null) {
                        return xmlResource;
                    }
                }
            }
        }
        // pr�fen, ob es noch untergeordnete Tags gibt:
        if (xmlResource.getChildTags().length > 0) {
            // alle childTags per Rekursion pr�fen:
            for (int i = 0; i < xmlResource.getChildTags().length; i++) {
                tagCrawlerResult = tagCrawlerFindFirstOf(xmlResource.getChildTagAtIndex(i), tagType, parameter);
            }
        }
        return tagCrawlerResult;
    }

    /**
     * @param xmlResource     Xml in dem gesucht werden soll
     * @param tagType         String der den zu suchenden Tag-Typen enth�lt
     * @param contentContains String, der den zu suchenden Content enth�lt
     * @return Xml mit Suchergebnis
     * @author Tobias Janssen Sucht in dem angegebenen XmlTag abw�rts nach dem
     * gew�nschten tagType mit entsprechendem Content
     * @deprecated
     */
    public Xml tagCrawlerFindFirstOf(Xml xmlResource, String tagType, String contentContains) {
        // pr�fen, ob das resultArray bereits initialisiert wurde:
        if (tagCrawlerResult != null) {
            return tagCrawlerResult;
        }
        // pr�fen, ob der tagType stimmt, und ob der Content �berhaupt daten
        // enth�lt
        if (xmlResource.getType().equalsIgnoreCase(tagType) && xmlResource.getDataContent() != null) {
            // den Datencontent auf den Suchstring �berpr�fen:
            if (xmlResource.getDataContent().contains(contentContains)) {
                // bei einem Fund diesen zum resultArray hinzuf�gen
                // resultArray =
                // (XmlTag[])ArrayOperations.AppendToArray(resultArray,currentTag);
                return xmlResource;
            }

        }
        // pr�fen, ob es noch untergeordnete Tags gibt:
        if (xmlResource.getChildTags().length > 0) {
            // alle childTags per Rekursion pr�fen:
            for (int i = 0; i < xmlResource.getChildTags().length; i++) {
                tagCrawlerResult = tagCrawlerFindFirstOf(xmlResource.getChildTagAtIndex(i), tagType, contentContains);

            }
        }
        return tagCrawlerResult;
    }

    /**
     * @param xmlResource Xml in dem gesucht werden soll
     * @param tagType     String der den zu suchenden Tag-Typen enth�lt
     * @author Tobias Janssen Sucht in dem angegebenen XmlTag abw�rts nach dem
     * gew�nschten tagType
     */
    public void tagCrawlerFindAllOf(Xml xmlResource, String tagType) {
        // pr�fen, ob das resultArray bereits initialisiert wurde:
        if (tagCrawlerResultArray == null) {
            // resultArray initialisieren
            tagCrawlerResultArray = new Xml[0];
        }
        // pr�fen, ob der tagType stimmt
        if (xmlResource.getType().equalsIgnoreCase(tagType)) {
            // bei einem Fund diesen zum resultArray hinzuf�gen
            tagCrawlerResultArray = (Xml[]) ArrayOperations.AppendToArray(tagCrawlerResultArray, xmlResource);
        }
        // pr�fen, ob es noch untergeordnete Tags gibt:
        if (xmlResource.getChildTags().length > 0) {
            // alle childTags per Rekursion pr�fen:
            for (int i = 0; i < xmlResource.getChildTags().length; i++) {
                tagCrawlerFindAllOf(xmlResource.getChildTagAtIndex(i), tagType);
            }
        }
    }

    /**
     * @param xmlResource Xml in dem gesucht werden soll
     * @param tagType     String der den zu suchenden Tag-Typen enth�lt
     * @return Xml mit Suchergebnis
     * @author Tobias Janssen Sucht in dem angegebenen XmlTag abw�rts nach dem
     * gew�nschten tagType, parameter & parameter value
     */
    public Xml tagCrawlerFindFirstEntryOf(Xml xmlResource, String tagType) {
        // pr�fen, ob das resultArray bereits initialisiert wurde:
        if (tagCrawlerResult != null) {
            // resultArray initialisieren
            return tagCrawlerResult;
        }
        // pr�fen, ob der tagType stimmt
        if (xmlResource.getType().equalsIgnoreCase(tagType)) {
            // bei einem Fund diesen zum resultArray hinzuf�gen
            return xmlResource;
        }
        // pr�fen, ob es noch untergeordnete Tags gibt und ob bereits ein
        // Ergebnis vorliegt:
        if (xmlResource.getChildTags().length > 0) {
            // alle childTags per Rekursion pr�fen:
            for (int i = 0; i < xmlResource.getChildTags().length; i++) {
                tagCrawlerResult = tagCrawlerFindFirstEntryOf(xmlResource.getChildTagAtIndex(i), tagType);
            }
        }
        return tagCrawlerResult;
    }

    /**
     * @param xmlResource Xml in dem gesucht werden soll
     * @return Xml mit Suchergebnis
     * @author Tobias Janssen Sucht nach dem untersten Child eines Xml tags
     */
    public Xml tagCrawlerFindDeepestChild(Xml xmlResource) {
        if (xmlResource.getChildTags().length > 0) {
            return tagCrawlerFindDeepestChild(xmlResource.getChildTagAtIndex(0));
        } else {
            return xmlResource;
        }
    }

    /**
     * @author Tobias Janssen Sucht nach dem untersten Child eines Xml tags,
     * dass noch keine suchId erhalten hat
     */
    public Xml tagCrawlerFindDeepestUnSumerizedChild(Xml xmlResource, int rndmId) {
        for (int i = 0; i < xmlResource.getChildTags().length; i++) {
            if (xmlResource.getChildTagAtIndex(i).getRandomId() != rndmId) {
                return tagCrawlerFindDeepestUnSumerizedChild(xmlResource.getChildTagAtIndex(i), rndmId);
            }
        }
        return xmlResource;
    }


}
