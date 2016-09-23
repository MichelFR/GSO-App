/*
 * Pager.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.ribeiro.android.gso.dataclasses;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.viewpagerindicator.TitlePageIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import de.ribeiro.android.gso.ICalEvent;
import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.R;
import de.ribeiro.android.gso.core.MyContext;
import de.ribeiro.android.gso.core.WeekData;

public class Pager {
    private final String[] timeslots = new String[]{"", "7.45 - 8.30", "8.30 - 9.15", "9.35 - 10.20", "10.20 - 11.05", "11.25 - 12.10",
            "12.10 - 12.55", "13.15 - 14.00", "14.00 - 14.45", "15.05 - 15.50", "15.50 - 16.35", "16.55 - 17.40", "17.40 - 18.25", "18.25 - 19.10",
            "19.30 - 20.15", "20.15 - 21.00"};
    private ViewPager viewPager;
    private List<View> pages = new ArrayList<View>();
    private List<Calendar> pageIndex = new ArrayList<Calendar>();
    private List<String> headlines = new ArrayList<String>();
    private PagerAdapter pageAdapter;
    private TitlePageIndicator pageIndicator;
    private Boolean isPagerInit = false;
    private int textSize;

    private LayoutInflater inflater;
    private Context context;
    private Logger _logger;
    private Boolean hideEmptyHours;

    /**
     * Erstellt ein Pager-Object
     *
     * @param context        Context der Applikation
     * @param viewPager      ViewPager Referenz
     * @param pageIndicator  TitlePageIndicaor Referenz
     * @param inflater       LayoutInflater der Applikation
     * @param textSize       Integer der zu verwendenen Schriftgr��e
     * @param logger         Logger object zum Fehler-logging
     * @param hideEmptyHours Boolean der angibt, ob Freistunden angezeigt werden, oder
     *                       nicht
     */
    public Pager(Context context, ViewPager viewPager, TitlePageIndicator pageIndicator, LayoutInflater inflater, int textSize, Boolean hideEmptyHours) {
        this.context = context;
        this.viewPager = viewPager;
        this.pageIndicator = pageIndicator;
        this.inflater = inflater;
        this.textSize = textSize;
        this._logger = new Logger(context, "Pager");
        this.hideEmptyHours = hideEmptyHours;
    }

    public int size() {
        return pages.size();
    }

    /**
     * Initialisiert den Pager mit dem �bergebenem Datum
     *
     * @param date
     */
    public void init(Calendar date) {
        try {
            int currentPage = getPage(date);
            pageAdapter = new MyPagerAdapter(pages, headlines);
            viewPager.setAdapter(pageAdapter);

            pageIndicator.setViewPager(viewPager);
            pageIndicator.invalidate();
            viewPager.setCurrentItem(currentPage, false);

        } catch (Exception e) {
            _logger.Error("Error creating Pager", e);
        }

        this.isPagerInit = true;
    }

    /**
     * Leert alle Daten des Pagers
     *
     * @author ribeiro
     */
    public void clear() {
        pages = new ArrayList<View>();
        pageIndex = new ArrayList<Calendar>();
        headlines = new ArrayList<String>();
        pageAdapter = new MyPagerAdapter(pages, headlines);

        viewPager.setAdapter(pageAdapter);
        pageIndicator.setViewPager(viewPager);

    }

    /**
     * Gibt an, ob der Pager inititalisiert wurde, die sist nach ausf�hren der
     * Methode init() der Fall
     *
     * @return
     */
    public Boolean isPagerInitialised() {
        return this.isPagerInit;
    }

    /**
     * F�gt die Page an die richtige Position im pager an
     *
     * @param currentWeek
     * @param page
     * @param header
     * @param startIndex
     * @param stopIndex
     * @author Tobias Janssen
     */
    private void insertWeekPage(Calendar currentWeek, View page, String header, int startIndex, int stopIndex) {
        // pr�fen, an welche stelle die page geh�rt
        // dazu die mitte aller bestehenden pages nehmen
        int midPos = ((stopIndex - startIndex) / 2) + startIndex;

        if (midPos == 0) {
            // es existiert keiner, oder max ein eintrag
            // daher pr�fen, ob ein eintrag besteht
            if (pageIndex.size() >= 1) {
                // ja, einen eintrag gibt es bereits
                int pageDate = calcIntYearDay(pageIndex.get(midPos));
                int currentDate = calcIntYearDay(currentWeek);

                // pr�fen, ob die bestehende seite "�lter" als die
                // hinzuzuf�gende ist
                if (pageDate < currentDate) {
                    // die page indexieren
                    pageIndex.add(midPos + 1, (Calendar) currentWeek.clone());
                    pages.add(midPos + 1, page);
                    headlines.add(midPos + 1, header);
                } else {
                    // die page indexieren
                    pageIndex.add(midPos, (Calendar) currentWeek.clone());
                    pages.add(midPos, page);
                    headlines.add(midPos, header);
                }
            } else {
                // nein es ist alles leer, daher einfach einf�gen
                // die page indexieren
                pageIndex.add(midPos, (Calendar) currentWeek.clone());
                pages.add(midPos, page);
                headlines.add(midPos, header);
            }
        } else {

            int pageDate = calcIntYearDay(pageIndex.get(midPos));
            int currentDate = calcIntYearDay(currentWeek);

            // pr�fen, ob die bestehende seite "�lter" als die hinzuzuf�gende
            // ist
            if (pageDate < currentDate) {
                // ja, ist �lter, daher muss die page auf jeden fall dahinder
                // eingef�gt werden
                // pr�fen, ob direkte nachbarschaft besteht
                // dazu erstmal pr�fen, ob der n�chste nachbar �berhaupt
                // existiert
                if (midPos + 1 >= pageIndex.size()) {
                    // existiert gar keiner mehr; daher page hinzuf�gen

                    // die page indexieren
                    pageIndex.add(midPos + 1, (Calendar) currentWeek.clone());
                    pages.add(midPos + 1, page);
                    headlines.add(midPos + 1, header);
                } else {
                    // es ist ein nachbar vorhanden
                    int pageNeighborDate = calcIntYearDay(pageIndex.get(midPos + 1));
                    // pr�fen, ob dieser n�her dran liegt als die currentPage
                    if (pageNeighborDate < currentDate) {
                        // ja alte page ist ein n�herer nachbar
                        insertWeekPage(currentWeek, page, header, midPos, stopIndex);
                    } else {
                        // nein, currentPage ist n�her
                        // also dazwischen einf�gen
                        // die page indexieren
                        pageIndex.add(midPos + 1, (Calendar) currentWeek.clone());
                        pages.add(midPos + 1, page);
                        headlines.add(midPos + 1, header);

                    }
                }

            } else {
                // nein,die bestehende seite ist hat ein j�ngers Datum als die
                // hinzuzuf�gende, daher muss die neue page auf jeden fall davor
                // eingef�gt werden

                if (midPos == 0) {
                    // existiert gar kein eintrag; daher page hinzuf�gen

                    // die page indexieren
                    pageIndex.add((Calendar) currentWeek.clone());
                    pages.add(page);
                    headlines.add(header);
                } else {
                    // pr�fen, ob der vorg�nger Nachbar kleiner ist
                    int pageNeighborDate = calcIntYearDay(pageIndex.get(midPos - 1));

                    if (pageNeighborDate < currentDate) {
                        // ja davorige page ist kleiner
                        // also dazwischen einf�gen
                        // die page indexieren
                        pageIndex.add(midPos, (Calendar) currentWeek.clone());
                        pages.add(midPos, page);
                        headlines.add(midPos, header);

                    } else {
                        insertWeekPage(currentWeek, page, header, 0, midPos);
                    }
                }
            }

        }

    }

    public void addView(int pos, View page, String headline) {
        pages.add(pos, page);
        headlines.add(pos, headline);
    }

    /**
     * F�gt die Page an die richtige Position im pager an
     *
     * @param currentDay
     * @param page
     * @param header
     * @param startIndex
     * @param stopIndex
     * @author Tobias Janssen
     */
    private void insertDayPage(Calendar currentDay, View page, String header, int startIndex, int stopIndex) {

        // pr�fen, an welche stelle die page geh�rt
        // dazu die mitte aller bestehenden pages nehmen
        int midPos = ((stopIndex - startIndex) / 2) + startIndex;

        if (midPos == 0) {
            // es existiert keiner, oder max ein eintrag
            // daher pr�fen, ob ein eintrag besteht
            if (pageIndex.size() >= 1) {
                // ja, einen eintrag gibt es bereits
                int pageDate = calcIntYearDay(pageIndex.get(midPos));
                int currentDate = calcIntYearDay(currentDay);

                // pr�fen, ob die bestehende seite "�lter" als die
                // hinzuzuf�gende ist
                if (pageDate < currentDate) {
                    // die page indexieren
                    pageIndex.add(midPos + 1, (Calendar) currentDay.clone());
                    pages.add(midPos + 1, page);
                    headlines.add(midPos + 1, header);
                } else {
                    // die page indexieren
                    pageIndex.add(midPos, (Calendar) currentDay.clone());
                    pages.add(midPos, page);
                    headlines.add(midPos, header);
                }
            } else {
                // nein es ist alles leer, daher einfach einf�gen
                // die page indexieren
                pageIndex.add(midPos, (Calendar) currentDay.clone());
                pages.add(midPos, page);
                headlines.add(midPos, header);
            }
        } else {
            // daten Tag des Jahres abrufen
            int pageDate = calcIntYearDay(pageIndex.get(midPos));
            int currentDate = calcIntYearDay(currentDay);

            // pr�fen, ob die bestehende seite "�lter" als die hinzuzuf�gende
            // ist
            if (pageDate < currentDate) {
                // ja, ist �lter, daher muss die page auf jeden fall dahinder
                // eingef�gt werden
                // pr�fen, ob direkte nachbarschaft besteht
                // dazu erstmal pr�fen, ob der n�chste nachbar �berhaupt
                // existiert
                if (midPos + 1 >= pageIndex.size()) {
                    // existiert gar keiner mehr; daher page hinzuf�gen

                    // die page indexieren
                    pageIndex.add(midPos + 1, (Calendar) currentDay.clone());
                    pages.add(midPos + 1, page);
                    headlines.add(midPos + 1, header);
                } else {
                    // es ist ein nachbar vorhanden
                    // pr�fen, ob dieser n�her dran liegt als die currentPage
                    int pageNeighborDate = calcIntYearDay(pageIndex.get(midPos + 1));
                    if (pageNeighborDate < currentDate) {
                        // ja alte page ist ein n�herer nachbar
                        insertDayPage(currentDay, page, header, midPos, stopIndex);
                    } else {
                        // nein, currentPage ist n�her
                        // also dazwischen einf�gen
                        // die page indexieren
                        pageIndex.add(midPos + 1, (Calendar) currentDay.clone());
                        pages.add(midPos + 1, page);
                        headlines.add(midPos + 1, header);

                    }
                }

            } else {
                // nein,die bestehende seite ist hat ein j�ngers Datum als die
                // hinzuzuf�gende, daher muss die neue page auf jeden fall davor
                // eingef�gt werden

                if (midPos == 0) {
                    // existiert gar kein eintrag; daher page hinzuf�gen

                    // die page indexieren
                    pageIndex.add((Calendar) currentDay.clone());
                    pages.add(page);
                    headlines.add(header);
                } else {
                    // pr�fen, ob der vorg�nger Nachbar kleiner ist
                    int pageNeighborDate = calcIntYearDay(pageIndex.get(midPos - 1));
                    if (pageNeighborDate < currentDate) {
                        // ja davorige page ist kleiner
                        // also dazwischen einf�gen
                        // die page indexieren
                        pageIndex.add(midPos, (Calendar) currentDay.clone());
                        pages.add(midPos, page);
                        headlines.add(midPos, header);

                    } else {
                        insertDayPage(currentDay, page, header, 0, midPos);
                    }
                }
            }

        }
    }

    /**
     * Ersetzt in der Liste der Pages und Headlines die Seiten die zu der
     * �bergebenen WeekData passen
     *
     * @param weekData
     * @author Tobias Janssen
     */
    public void replaceTimeTableInPager(WeekData weekData) {
        Calendar currentDay = new GregorianCalendar();
        currentDay = (Calendar) weekData.date.clone();
        int currentDayOfWeek = currentDay.get(Calendar.DAY_OF_WEEK);
        while (currentDayOfWeek != 2) {
            // currentDay.roll(Calendar.DAY_OF_YEAR, false);
            currentDay.setTimeInMillis(currentDay.getTimeInMillis() + 86400000);
        }

        if (context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            View page = createWeekPage(weekData);
            String header = createWeekHeader(weekData, currentDay);
            // location suchen
            int location = -1;
            for (int i = 0; i < headlines.size() && location == -1; i++) {
                if (headlines.get(i).equals(header))
                    location = i;
            }
            if (location == -1)
                location = 0;
            pages.set(location, page);
            headlines.set(location, header);
            currentDay.setTimeInMillis(currentDay.getTimeInMillis() + 86400000);

        } else {
            for (int x = 1; x < weekData.timetable[0].length; x++) {
                List<TimetableViewObject> list = createTimetableDayViewObject(weekData, currentDay);
                View page = createPage(weekData, list);
                String header = createDayHeader(weekData, currentDay);

                // location suchen
                int location = -1;
                for (int i = 0; i < headlines.size() && location == -1; i++) {
                    if (headlines.get(i).equals(header))
                        location = i;
                }
                if (location == -1)
                    location = 0;
                pages.set(location, page);
                headlines.set(location, header);

                currentDay.setTimeInMillis(currentDay.getTimeInMillis() + 86400000);
                // currentDay.roll(Calendar.DAY_OF_YEAR,true);
            }
        }
    }

    /**
     * Erstellt den �berschriften String
     *
     * @param weekData
     * @param currentDay
     * @return
     * @author Tobias Janssen
     */
    private String createDayHeader(WeekData weekData, Calendar currentDay) {
        int x = currentDay.get(Calendar.DAY_OF_WEEK);

        return getDayNameOfDayofWeek(x) + " " + currentDay.get(Calendar.DAY_OF_MONTH) + "." + (currentDay.get(Calendar.MONTH) + 1) + "."
                + currentDay.get(Calendar.YEAR);
    }

    private String getDayNameOfDayofWeek(int value) {
        if (value == 1)
            return "So";
        if (value == 2)
            return "Mo";
        if (value == 3)
            return "Di";
        if (value == 4)
            return "Mi";
        if (value == 5)
            return "Do";
        if (value == 6)
            return "Fr";
        if (value == 7)
            return "Sa";
        return "";
    }

    /**
     * Erstellt eine Seite des ViewPagers, inkl Header und Footer
     *
     * @param weekData
     * @param ctxt
     * @param list
     * @return
     * @author Tobias Janssen
     */
    private View createPage(WeekData weekData, List<TimetableViewObject> list) {

        try {
            View page = inflater.inflate(R.layout.daylayout, null);
            ListView listView = (ListView) page.findViewById(R.id.listTimetable);
            MyListAdapter adapter = new MyListAdapter(context, list);
            listView.setAdapter(adapter);

            TextView syncTime = (TextView) page.findViewById(R.id.syncTime);
            Calendar sync = new GregorianCalendar();
            sync.setTimeInMillis(weekData.syncTime);

            String minute = String.valueOf(sync.get(Calendar.MINUTE));
            if (minute.length() == 1)
                minute = "0" + minute;

            syncTime.setText(weekData.elementId + " | Stand vom " + sync.get(Calendar.DAY_OF_MONTH) + "." + (sync.get(Calendar.MONTH) + 1) + "."
                    + sync.get(Calendar.YEAR) + " " + sync.get(Calendar.HOUR_OF_DAY) + ":" + minute + " Uhr");
            return page;
        } catch (Exception e) {
            _logger.Error("Error inflating Page", e);
        }
        return null;
    }

    private String ResolveWeekDay(int value) {
        switch (value) {
            case Calendar.MONDAY:
                return "Montag";
            case Calendar.TUESDAY:
                return "Dienstag";
            case Calendar.WEDNESDAY:
                return "Mittwoch";
            case Calendar.THURSDAY:
                return "Donnerstag";
            case Calendar.FRIDAY:
                return "Freitag";
            case Calendar.SATURDAY:
                return "Samstag";
            case Calendar.SUNDAY:
                return "Sonntag";
            default:
                return String.valueOf(value);
        }

    }

    private TextView addColumn(String text, String color, TableRow tr) {
        LinearLayoutBordered ll = new LinearLayoutBordered(context);
        ll = new LinearLayoutBordered(context);
        ll.setBorderRight(true);
        ll.setBorderBottom(true);
        ll.setBorderTop(true);
        ll.setBorderSize(1);
        ll.setBackgroundColor(Color.WHITE);

        View textview = inflater.inflate(R.layout.textview, null);
        TextView tv = (TextView) textview.findViewById(R.id.textview);
        // �berschriftentextgr��e einstellen

        tv.setTextSize(textSize);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        ll.addView(tv);
        tr.addView(ll);
        return tv;
    }

    /**
     * Erstellt eine Stundeplan Seite des ViewPagers, inkl Header und Footer
     * <p/>
     * Hier wird die Wochenansicht generiert
     *
     * @param weekData
     * @param ctxt
     * @return
     * @author Tobias Janssen
     */
    private View createWeekPage(WeekData weekData) {
        // in die Page kommen alle Elemente dieser Ansicht
        View page = inflater.inflate(R.layout.weeklayout, null);

        TableLayout tl = (TableLayout) page.findViewById(R.id.weekTimetable);
        LinearLayoutBordered ll = new LinearLayoutBordered(context);

        // Tages�berschrift erstellen:
        TableRow tr = new TableRow(context);

        for (int x = Calendar.SUNDAY; x < Calendar.SATURDAY; x++) {
            // einen neuen Rahmen f�r das Tabellenfeld vorbereiten
            ll = new LinearLayoutBordered(context);
            ll.setBorderRight(true);
            ll.setBorderBottom(true);
            ll.setBorderTop(true);
            ll.setBorderSize(1);
            ll.setBackgroundColor(Color.WHITE);

            View textview = inflater.inflate(R.layout.textview, null);
            TextView tv = (TextView) textview.findViewById(R.id.textview);
            // �berschriftentextgr��e einstellen

            tv.setTextSize(textSize);
            if (x == Calendar.SUNDAY) {
                tv.setText(timeslots[0]);
                tv.setTextColor(Color.parseColor("#3A599A"));

            } else {
                tv.setText(ResolveWeekDay(x));
            }
            ll.addView(tv);
            tr.addView(ll);
        }
        tl.addView(tr);

        // den Stundenplan zusammensetzten
        // f�r jeden tag
        List<Lesson> stunden = GetSchulstunden();
        for (int y = 0; y < stunden.size(); y++) {
            tr = new TableRow(context);
            for (int x = Calendar.SUNDAY; x <= Calendar.FRIDAY; x++) {
                if (x == Calendar.SUNDAY) {
                    addColumn(timeslots[y + 1], "#3A599A", tr);
                } else {
                    // alle events dieses Tages durchgehen ob die zu dieser
                    // schulstunde passen
                    boolean lessonAdded = false;
                    TextView lastTextView = null;
                    for (ICalEvent ev : weekData.events) {
                        // ist event an diesem tag?
                        if (ev.DTSTART.get(Calendar.DAY_OF_WEEK) == x) {
                            // ja
                            // ist event zu dieser schulstunde?

                            Time st = new Time();
                            st.set(ev.DTSTART.getTimeInMillis());
                            int start = GetSchulstundeOfDateTime(st);
                            Time et = new Time();
                            et.set(ev.DTEND.getTimeInMillis() - 60000);
                            int end = GetSchulstundeOfDateTime(et);
                            // ende der schulstunde herausfinden
                            if (((start != end) && y >= start && y <= end) || start == y || end == y) {
                                // ja event ist in dieser stunde
                                // ist eine Doopelbelegung f�r diese Stunde?
                                if (lessonAdded && lastTextView != null) {
                                    // ja, doppelbelegung
                                    String newText = "";
                                    if (weekData.typeId.equalsIgnoreCase("4")) {
                                        newText = lastTextView.getText() + "\r\n" + ev.DESCRIPTION + " " + ev.SUMMARY;
                                    } else {
                                        newText = lastTextView.getText() + "\r\n" + ev.DESCRIPTION.replace(weekData.elementId, "") + " " + ev.SUMMARY
                                                + " " + ev.LOCATION;
                                    }
                                    lastTextView.setText(newText);
                                } else {
                                    // pr�fen, ob dieses event eine gel�schte
                                    // stunde ist
                                    if (ev.UID.equalsIgnoreCase("deleted")) {
                                        // gel�schtes event
                                        lastTextView = addColumn(" --- " + " " + " --- " + " " + " --- ", "#FF0000", tr);
                                    } else {
                                        String color = "#3A599A";
                                        if (ev.UID.equalsIgnoreCase("diff"))
                                            color = "#FF0000";
                                        if (weekData.typeId.equalsIgnoreCase("4")) {
                                            lastTextView = addColumn(ev.DESCRIPTION + " " + ev.SUMMARY, color, tr);
                                        } else {
                                            lastTextView = addColumn(ev.DESCRIPTION.replace(weekData.elementId, "") + " " + ev.SUMMARY + " "
                                                    + ev.LOCATION, color, tr);
                                        }
                                    }
                                }

                                lessonAdded = true;
                            }
                        }
                    }
                    if (!lessonAdded) {
                        // ja event ist in dieser stunde
                        addColumn("", "#3A599A", tr);
                    }
                }
            }
            tl.addView(tr);
        }
        TextView syncTime = (TextView) page.findViewById(R.id.syncTime);
        Calendar sync = new GregorianCalendar();
        sync.setTimeInMillis(weekData.syncTime);

        String minute = String.valueOf(sync.get(Calendar.MINUTE));
        if (minute.length() == 1)
            minute = "0" + minute;

        syncTime.setText(weekData.elementId + " | Stand vom " + sync.get(Calendar.DAY_OF_MONTH) + "." + (sync.get(Calendar.MONTH) + 1) + "."
                + sync.get(Calendar.YEAR) + " " + sync.get(Calendar.HOUR_OF_DAY) + ":" + minute + " Uhr");

        return page;
    }

    /**
     * @param weekData
     * @param currentWeek
     * @return
     * @author Tobias Janssen
     * <p/>
     * Erstellt den �berschriften String
     */
    private String createWeekHeader(WeekData weekData, Calendar currentWeek) {
        int firstDay = currentWeek.get(Calendar.DAY_OF_MONTH);
        Calendar cal = (Calendar) currentWeek.clone();
        // den aktuellen Wochentag abrufen
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // den currentDay auf Montag setzten
        if (currentDayOfWeek < 6) {
            // 1000*60*60*24 = 1 Tag!
            cal.setTimeInMillis(cal.getTimeInMillis() + (1000 * 60 * 60 * 24 * (6 - currentDayOfWeek)));
        }
        int lastDay = cal.get(Calendar.DAY_OF_MONTH);
        return firstDay + "." + (currentWeek.get(Calendar.MONTH) + 1) + " - " + lastDay + "." + (cal.get(Calendar.MONTH) + 1) + "."
                + cal.get(Calendar.YEAR);
    }

    /**
     * f�gt der Liste der Pages und Headlines den �bergebenen TimeTable hinzu
     *
     * @param weekData
     * @param ctxt
     * @author Tobias Janssen
     */
    public void appendTimeTableToPager(WeekData weekData, MyContext ctxt) {
        // eine Kopie des Stundenplan-Datums erstellen
        Calendar currentDay = new GregorianCalendar();
        currentDay = (Calendar) weekData.date.clone();

        // den aktuellen Wochentag abrufen
        int currentDayOfWeek = currentDay.get(Calendar.DAY_OF_WEEK);

        if (currentDayOfWeek == Calendar.SUNDAY) {
            // 1000*60*60*24 = 1 Tag!
            currentDay.setTimeInMillis(currentDay.getTimeInMillis() + (1000 * 60 * 60 * 24));
        }
        // den currentDay auf Montag setzten
        if (currentDayOfWeek > Calendar.MONDAY) {
            // 1000*60*60*24 = 1 Tag!
            currentDay.setTimeInMillis(currentDay.getTimeInMillis() - (1000 * 60 * 60 * 24 * (currentDayOfWeek - 2)));
        }
        if (context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            View page = createWeekPage(weekData);
            insertWeekPage(currentDay, page, createWeekHeader(weekData, currentDay), 0, ctxt.pager.pageIndex.size());

            // currentDay.roll(Calendar.WEEK_OF_YEAR,true);
            currentDay.setTimeInMillis(currentDay.getTimeInMillis() + 86400000);

        } else {
            // alle Tage der Woche hinzuf�gen
            for (int x = Calendar.MONDAY; x < Calendar.SATURDAY; x++) {
                // eine Tagesansicht erstellen
                List<TimetableViewObject> list = createTimetableDayViewObject(weekData, currentDay);

                View page = createPage(weekData, list);
                insertDayPage(currentDay, page, createDayHeader(weekData, currentDay), 0, ctxt.pager.pageIndex.size());

                // currentDay.roll(Calendar.DAY_OF_YEAR,1);
                // einen tag weiter vor
                currentDay.setTimeInMillis(currentDay.getTimeInMillis() + 86400000);
            }
        }

    }

    /**
     * Kombiniert Jahr und dem DAY_OF_YEAR des �bergebenen Calendars einen
     * Integer , der leichter zu vergleichen ist
     *
     * @param calendar Calendar aus dem der Wert erzeugt werden soll
     * @return Integer mit dem umgerechneten Wert (z.B.: aus Jahr 2013 und dem
     * Tag 300 wird 2013300)
     * @author Tobias Janssen
     */
    private int calcIntYearDay(Calendar calendar) {
        return (calendar.get(Calendar.YEAR) * 1000) + calendar.get(Calendar.DAY_OF_YEAR);
    }

    private List<Lesson> GetSchulstunden() {
        // local time
        List<Lesson> schulstunden = new ArrayList<Lesson>();
        schulstunden.add(new Lesson(7, 45, 8, 30));
        schulstunden.add(new Lesson(8, 30, 9, 15));
        schulstunden.add(new Lesson(9, 35, 10, 20));
        schulstunden.add(new Lesson(10, 20, 11, 5));
        schulstunden.add(new Lesson(11, 25, 12, 10));
        schulstunden.add(new Lesson(12, 10, 12, 55));
        schulstunden.add(new Lesson(13, 15, 14, 0));
        schulstunden.add(new Lesson(14, 0, 14, 45));
        schulstunden.add(new Lesson(15, 5, 15, 50));
        schulstunden.add(new Lesson(15, 50, 16, 35));
        schulstunden.add(new Lesson(16, 55, 17, 40));
        schulstunden.add(new Lesson(17, 40, 18, 25));
        schulstunden.add(new Lesson(18, 25, 19, 10));
        schulstunden.add(new Lesson(19, 30, 20, 15));
        schulstunden.add(new Lesson(20, 15, 21, 0));
        return schulstunden;
    }

    private int GetSchulstundeOfEvent(ICalEvent event) {
        List<Lesson> schulstunden = GetSchulstunden();
        for (int std = 0; std < schulstunden.size(); std++) {
            // pr�fen, ob anfang und ende innerhalb von zwei schulstunden liegt

            if (event.DTSTART.get(Calendar.HOUR_OF_DAY) >= schulstunden.get(std).Start.hour
                    && event.DTSTART.get(Calendar.HOUR_OF_DAY) <= schulstunden.get(std).End.hour) {
                // wenn alles innerhalb einer stunde liegt muss noch die minute
                // verglichen werden
                if (event.DTSTART.get(Calendar.HOUR_OF_DAY) == schulstunden.get(std).End.hour) {
                    if (event.DTSTART.get(Calendar.MINUTE) >= schulstunden.get(std).Start.minute
                            && event.DTSTART.get(Calendar.MINUTE) < schulstunden.get(std).End.minute) {
                        return std;
                    }
                } else {
                    return std;
                }
            }
        }
        return -1;
    }

    private int CompareTime(Time a, Time b) {
        if (a.hour < b.hour)
            return -1;
        if (a.hour > b.hour)
            return 1;
        if (a.minute < b.minute)
            return -1;
        if (a.minute > b.minute)
            return 1;
        return 0;
    }

    private int GetSchulstundeOfDateTime(Time time) {
        List<Lesson> schulstunden = GetSchulstunden();
        for (int std = 0; std < schulstunden.size(); std++) {
            // pr�fen, ob anfang und ende innerhalb von zwei schulstunden liegt
            int begin = CompareTime(time, schulstunden.get(std).Start); // -1 =
            // vor
            // der
            // schulstunde
            // 0 =
            // schulstunde
            // 1
            // nach
            // der
            // schulstunde
            int max = CompareTime(time, schulstunden.get(std).End); // -1 = vor
            // der
            // schulstunde
            // 0 =
            // schulstunde
            // 1 nach
            // der
            // schulstunde
            // begin muss 0 oder 1 sein & max muss -1 oder 0 sein, dann ist
            // stunde gefunden

            if (begin == 0 || (begin == 1 && max == -1)) {
                return std;
            }
        }
        return -1;
    }

    /**
     * * Erzeugt aus dem WeekData-Objekt eine List aus TimeTableViewObject
     *
     * @param weekData
     * @param currentDay
     * @return
     * @author Tobias Janssen
     */
    private List<TimetableViewObject> createTimetableDayViewObject(WeekData weekData, Calendar currentDay) {

        List<TimetableViewObject> result = new ArrayList<TimetableViewObject>();

        List<Lesson> schulstunden = GetSchulstunden();

        currentDay.setTimeZone(TimeZone.getTimeZone("UTC"));

        // leeren Stundenplan erstellen
        for (int std = 0; std < schulstunden.size(); std++) {
            result.add(new TimetableViewObject(timeslots[std + 1], "", "#000000"));
        }
        boolean nothingAdded = true;
        // alle events durchgehen
        for (int i = 0; i < weekData.events.size(); i++) {
            ICalEvent event = weekData.events.get(i);
            // pr�fen, ob event im gew�nschten Jahr und tag ist
            if (event.DTSTART.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR)
                    && event.DTSTART.get(Calendar.DAY_OF_YEAR) == currentDay.get(Calendar.DAY_OF_YEAR)) {
                // ja, dann schulstunde des events herausfinden
                Time st = new Time();
                st.set(event.DTSTART.getTimeInMillis());
                int start = GetSchulstundeOfDateTime(st);
                Time et = new Time();
                et.set(event.DTEND.getTimeInMillis() - 60000);
                int end = GetSchulstundeOfDateTime(et);
                // ende der schulstunde herausfinden

                if (start != -1) {
                    for (int h = start; h <= end; h++) {
                        nothingAdded = false;
                        if (result.get(h).row2 == "") {
                            if (event.UID.equalsIgnoreCase("deleted")) {
                                result.set(h, new TimetableViewObject(timeslots[h + 1], "---" + " --- " + " --- ", "#FF0000"));
                            } else {
                                String color = "#000000";
                                if (event.UID.equalsIgnoreCase("diff"))
                                    color = "#FF0000";
                                if (weekData.typeId.equalsIgnoreCase("4")) {
                                    result.set(h, new TimetableViewObject(timeslots[h + 1], event.DESCRIPTION + " " + event.SUMMARY, color));
                                } else {
                                    result.set(h, new TimetableViewObject(timeslots[h + 1], event.DESCRIPTION.replace(weekData.elementId, "") + " "
                                            + event.SUMMARY + " " + event.LOCATION, color));
                                }
                            }
                        } else {
                            // Stundendoppelbelegung

                            if (weekData.typeId.equalsIgnoreCase("4")) {
                                result.get(h).row2 += "\r\n" + event.DESCRIPTION + " " + event.SUMMARY + " ";
                            } else {
                                result.get(h).row2 += "\r\n" + event.DESCRIPTION.replace(weekData.elementId, "") + " " + event.SUMMARY + " "
                                        + event.LOCATION;
                            }
                        }
                    }
                } else
                    _logger.Error("No matching lesson hour  for event found " + event.DTSTART.getTime());
            }
        }

        if (hideEmptyHours) {
            int i = 0;
            boolean done = false;
            while (!done) {
                if (i < result.size()) {
                    if (result.get(i).row2.equalsIgnoreCase("")) {
                        result.remove(i);
                    } else {
                        i++;
                    }
                } else
                    done = true;

            }
        }

        // pr�fen, ob gar keine Stunden vorhanden sind
        if (nothingAdded) {
            result.clear();
            result.add(new TimetableViewObject("", "kein Unterricht", "#000000"));
        }

        return result;
    }

    // /**
    // * * Erzeugt aus dem WeekData-Objekt eine List aus TimeTableViewObject
    // * @author Tobias Janssen
    // * @param weekData
    // * @param currentDay
    // * @return
    // */
    // private List<TimetableViewObject> createTimetableDayViewObject(WeekData
    // weekData, Calendar currentDay)
    // {
    //
    // int x = currentDay.get(Calendar.DAY_OF_WEEK) - 1;
    // List<TimetableViewObject> list = new ArrayList<TimetableViewObject>();
    //
    // int nullCounter = 0;
    // Boolean entryFound = false;
    // for (int y = 1; y < weekData.timetable.length; y++)
    // {
    //
    // if (weekData.timetable[y][x].getDataContent() == null && !entryFound &&
    // hideEmptyHours)
    // {
    // nullCounter++;
    // }
    // else if (weekData.timetable[y][x].getDataContent() != null)
    // {
    // if (weekData.timetable[y][x].getDataContent().equalsIgnoreCase("null") &&
    // !entryFound && hideEmptyHours)
    // {
    // nullCounter++;
    // }
    // else if (weekData.timetable[y][x].getDataContent().equalsIgnoreCase("")
    // && !entryFound && hideEmptyHours)
    // {
    // nullCounter++;
    // }
    // else
    // {
    // if (y != 0)
    // entryFound = true;
    // if (weekData.timetable[y][x].getDataContent().equalsIgnoreCase("null"))
    // {
    // list.add(new TimetableViewObject(timeslots[y], "", "#000000"));
    // }
    // else
    // {
    // String color = weekData.timetable[y][x].getColorParameter();
    // list.add(new TimetableViewObject(timeslots[y],
    // weekData.timetable[y][x].getDataContent()
    // .replaceAll("\n", " "), color));
    // }
    // }
    // }
    // else
    // {
    // list.add(new TimetableViewObject(timeslots[y], "", "#000000"));
    // }
    // }
    //
    // if (!hideEmptyHours)
    // {
    // // pr�fen, ob gar keine Stunden vorhanden sind
    // for (int i = 0; i < list.size(); i++)
    // {
    // if (list.get(i).row2.equalsIgnoreCase(""))
    // nullCounter++;
    // }
    // }
    //
    // // pr�fen, ob gar keine Stunden vorhanden sind
    // if (nullCounter == 15)
    // {
    // list.clear();
    // list.add(new TimetableViewObject("", "kein Unterricht", "#000000"));
    // }
    //
    // // nun von hinten aufrollen und alle leeren Stunden entfernen
    // TimetableViewObject lineObject;
    // for (int i = list.size() - 1; i >= 0; i--)
    // {
    // lineObject = list.get(i);
    // if (lineObject.row2.equalsIgnoreCase(""))
    // list.remove(i);
    // else
    // break;
    // }
    // return list;
    // }

    /**
     * Gibt den Wert der aktuellen Seitenzahl zur�ck
     *
     * @return
     */
    public int getCurrentPage() {
        return viewPager.getCurrentItem();
    }

    /**
     * Gibt das Datum der aktuellen Seite zur�ck
     *
     * @return
     */
    public Calendar getDateOfCurrentPage() {
        if (pageIndex.size() == 0)
            return null;
        return pageIndex.get(getCurrentPage());
    }

    /**
     * Setzt den Pager auf die �bergebene Seitenzahl
     *
     * @param page
     */
    public void setPage(int page) {
        viewPager.setCurrentItem(page);
        viewPager.refreshDrawableState();
    }

    public int getPage(Calendar currentDate) {
        return getPage(currentDate, size() - 1);
    }

    /**
     * Liefert den pageIndex des �bergegebenen Datums
     *
     * @param currentDate
     * @return
     * @author ribeiro
     */
    public int getPage(Calendar currentDate, int defaultReturn) {
        int currentDayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK);
        // den currentDay auf den folge Montag setzten
        if (currentDayOfWeek < 2) {
            // 1000*60*60*24 = 1 Tag!
            currentDate.setTimeInMillis(currentDate.getTimeInMillis() + (1000 * 60 * 60 * 24 * (2 - currentDayOfWeek)));
        }
        if (currentDayOfWeek > 6) {
            // 1000*60*60*24 = 1 Tag!
            currentDate.setTimeInMillis(currentDate.getTimeInMillis() + (1000 * 60 * 60 * 24 * 2));
        }

        int dayOfYearcurrent = 0;
        int weekOfYearcurrent = 0;
        int dayOfYearpage = 0;
        int weekOfYearpage = 0;
        int yearCurrent = 0;
        int yearPage = 0;

        int nextPage = defaultReturn;
        // alle Seiten des Pages durchlaufen und das Datum abfragen und mit dem
        // gew�nschten Datum vergleichen
        for (int i = 0; i < pageIndex.size(); i++) {
            dayOfYearcurrent = currentDate.get(Calendar.DAY_OF_YEAR);
            weekOfYearcurrent = currentDate.get(Calendar.WEEK_OF_YEAR);
            yearCurrent = currentDate.get(Calendar.YEAR);
            dayOfYearpage = pageIndex.get(i).get(Calendar.DAY_OF_YEAR);
            weekOfYearpage = pageIndex.get(i).get(Calendar.WEEK_OF_YEAR);
            yearPage = pageIndex.get(i).get(Calendar.YEAR);

            // pr�fen auf was getestet werden soll(wochenansicht, oder
            // tagesansicht)
            if (context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                // Tagesansicht
                if (yearPage < yearCurrent) {
                    nextPage = i;
                    if (dayOfYearpage < dayOfYearcurrent)
                        nextPage = i;
                }
                if ((dayOfYearcurrent == dayOfYearpage) && (yearCurrent == yearPage))
                    return i;
                else if ((dayOfYearcurrent < dayOfYearpage) && (yearCurrent == yearPage))
                    return i;
            } else if (context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                // Wochenansicht
                if (yearPage < yearCurrent) {
                    nextPage = i;
                    if (dayOfYearpage < dayOfYearcurrent)
                        nextPage = i;
                }
                if ((weekOfYearcurrent == weekOfYearpage) && (yearCurrent == yearPage))
                    return i;
                else if ((weekOfYearcurrent < weekOfYearpage) && (yearCurrent == yearPage))
                    return i;
            }
        }

        // dies kommt nur vor, wenn die Seite nicht gefunden wurde. dann wird
        // die n�chst kleinere Seite zur�ckgeliefert
        return nextPage;

    }

    /**
     * Setzt den Pager auf die Seite mit dem �bergebenen Datum
     *
     * @param date
     */
    public void setPage(Calendar date) {
        int page = getPage(date);
        viewPager.setCurrentItem(page);
        viewPager.refreshDrawableState();
    }

}
