package com.aim.framework;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Created by dcook on 2/12/15.
 */
public class EspressoTest extends ActivityInstrumentationTestCase2<HomeActivity> {

    public EspressoTest() {
        super(HomeActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Espresso will not launch our activity for us, we must launch it via getActivity().
        getActivity();
    }

    public void testSimpleClickAndCheckText() {


    }
}
