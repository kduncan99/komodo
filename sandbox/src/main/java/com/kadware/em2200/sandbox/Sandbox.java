package com.kadware.em2200.sandbox;

public class Sandbox {

    static public void main(
        final String[] args
    ) {
        long limit = System.currentTimeMillis() + 1000;
        int count = 0;
        while (System.currentTimeMillis() < limit) {
            ++count;
        }
        System.out.println(count);
    }
}
