/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Enter something");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String s = sc.nextLine();
            if (s.equals("quit")) {
                break;
            }
            System.out.println(s);
        }
    }
}
