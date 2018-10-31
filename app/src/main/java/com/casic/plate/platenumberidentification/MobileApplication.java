package com.casic.plate.platenumberidentification;

import android.app.Application;

import pr.platerecognization.IdentificationInit;


/**
 * Created by pchsun on 2018/10/30.
 */

public class MobileApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        IdentificationInit.initOpenCV();
        IdentificationInit.initRecognizer(this);
    }
}
