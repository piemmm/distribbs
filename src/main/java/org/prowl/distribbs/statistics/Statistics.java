package org.prowl.distribbs.statistics;

import org.prowl.distribbs.statistics.types.MHeard;
import org.prowl.distribbs.statistics.types.UnHeard;

public class Statistics {

    private MHeard mHeard;
    private UnHeard unHeard;

    public Statistics() {

        mHeard = new MHeard();
        unHeard = new UnHeard();

    }

    public MHeard getHeard() {
        return mHeard;
    }

    public UnHeard getUnHeard() {
        return unHeard;
    }

}
