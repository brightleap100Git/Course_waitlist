package com.example.coursewaitlist;

import com.example.coursewaitlist.cli.DemoConfiguration;
import java.io.IOException;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        DemoConfiguration.createCli(System.in, System.out).run();
    }
}
