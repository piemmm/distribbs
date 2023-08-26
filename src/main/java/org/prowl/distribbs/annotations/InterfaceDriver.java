package org.prowl.distribbs.annotations;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceDriver {

    String name();

    String description();

    String uiName();

}
