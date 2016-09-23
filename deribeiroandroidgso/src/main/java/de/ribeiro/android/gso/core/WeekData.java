/*
 * WeekData.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.ribeiro.android.gso.core;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.ribeiro.android.gso.ArrayOperations;
import de.ribeiro.android.gso.ICalEvent;
import de.ribeiro.android.gso.dataclasses.Parameter;
import de.ribeiro.android.gso.xml.Xml;

public class WeekData {
    private final String VERSION = "1";
    public String elementId = "";
    public String typeId = "";
    public String weekId = "";
    public Calendar date = new GregorianCalendar();
    public long syncTime = -1L;
    public long lastHtmlModified = 0;
    public String weekDataVersion = "";
    public Boolean isDirty = false;
    public Parameter[] parameters = new Parameter[0];
    public Stupid parent;
    public Xml[][] timetable; // wird f�r den Stundenplan ben�tigt
    public List<ICalEvent> events;

    public WeekData(Stupid stupidCore) {
        this.parent = stupidCore;
        this.events = new ArrayList<ICalEvent>();
    }

    /**
     * @author Tobias Janssen
     */
    public void setSyncDate() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        syncTime = date.getTime();
        weekDataVersion = VERSION;
        addParameter("syncTime", String.valueOf(date.getTime()));
        addParameter("weekDataVersion", VERSION);

    }

    /**
     * Vergleicht zwei WeekData Objekte auf �nderung und liefert die Fundstelle
     *
     * @param wd
     */
    public List<Point> compare(WeekData wd) {
        List<Point> result = new ArrayList<Point>();
        int y = 0;
        for (int x = 0; x < this.timetable[y].length && x < wd.timetable[y].length; x++) {
            while (y < this.timetable.length && y < wd.timetable.length) {
                if (this.timetable[y][x] != null && wd.timetable[y][x] != null) {
                    String colorthis = this.timetable[y][x].getColorParameter();
                    String colorwd = wd.timetable[y][x].getColorParameter();
                    if (!colorthis.equalsIgnoreCase(colorwd)) {
                        // �nderung gefunden
                        result.add(new Point(x, y));
                        //der tag kann somit abgehakt werden, denn eine �bereinstimmung pro tag ist genug
                        break;
                    }
                }
                y++;
            }
            y = 0;
        }
        return result;
    }

    /**
     * Pr�ft, ob der Stundeplan Abweichungen zu normalen Stundenpl�nen enth�lt anhand der Farbe
     *
     * @param wd
     */
    public List<Point> checkForChanges() {
        List<Point> result = new ArrayList<Point>();
        int y = 0;
        for (int x = 0; x < this.timetable[y].length; x++) {
            while (y < this.timetable.length) {
                if (this.timetable[y][x] != null) {
                    String colorthis = this.timetable[y][x].getColorParameter();
                    if (!colorthis.equalsIgnoreCase("#000000")) {
                        // �nderung gefunden
                        result.add(new Point(x, y));
                        //der tag kann somit abgehakt werden, denn eine �bereinstimmung pro tag ist genug
                        break;
                    }
                }
                y++;
            }
            y = 0;
        }
        return result;
    }

    /**
     * @param name  String der Name/Bezeichnung des Parameters
     * @param value String der Wert des Parameters
     * @author Tobias Janssen
     * F�gt dem WeekData Object weitere Parameter hinzu
     */
    public void addParameter(String name, String value) {
        Parameter parameter = new Parameter(name, value);
        int index = -1;
        for (int i = 0; i < parameters.length && index == -1; i++) {
            if (parameters[i].getName().equalsIgnoreCase(parameter.getName())) {
                index = i;
            }
        }
        if (index == -1) {
            parameters = (Parameter[]) ArrayOperations.AppendToArray(parameters, parameter);
        } else {
            parameters[index] = parameter;
        }

    }
}
