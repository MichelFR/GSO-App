package de.ribeiro.android.gso.core;

import java.util.ArrayList;
import java.util.List;

import de.ribeiro.android.gso.dataclasses.SelectOptions;

public class Type {
    public List<SelectOptions> elementList = new ArrayList<SelectOptions>();
    public List<SelectOptions> weekList = new ArrayList<SelectOptions>();
    public String typeName;
    public String type;


    /**
     * Liefert den Index passenend zu der
     * angegebenen KW aus den Online verf�gaberen Wochen zur�ck
     * <p/>
     * Wenn online nicht verf�gbar, wird -1 zur�ckgeliefert
     *
     * @param weekOfYear
     * @return int
     * @autor: @author Tobias Janssen
     */
    @Deprecated
    public int getIndexFromWeekList(String weekOfYear) {
        for (int i = 0; i < this.weekList.size(); i++) {
            if (weekOfYear.equalsIgnoreCase(this.weekList.get(i).index))
                return i;
        }
        return -1;
    }
}
