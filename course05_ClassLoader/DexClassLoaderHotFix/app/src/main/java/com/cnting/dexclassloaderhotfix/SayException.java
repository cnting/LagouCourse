package com.cnting.dexclassloaderhotfix;

/**
 * Created by cnting on 2020/6/16
 */
class SayException implements ISay {

    @Override
    public String saySomething() {
        return "something wrong";
    }
}
