package de.ribeiro.android.gso.dataclasses;

import android.text.format.Time;

public class Lesson {
    public Time Start = new Time();
    public Time End = new Time();

    public Lesson(int starthour, int startminute, int endhour, int endminute) {
        Start = new Time("UTC");
        Start.hour = starthour;
        Start.minute = startminute;
        End = new Time("UTC");
        End.hour = endhour;
        End.minute = endminute;
    }
}
