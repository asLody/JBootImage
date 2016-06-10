package com.kiva;

import java.io.IOException;

public class Main {

    public static void main(String... args) throws IOException {
//        for (int i = 0; i < 2; ++i) {
//            BootImage img = new BootImage("/Users/kiva/ss/mtk" + i + ".img");
//            img.unpack("/Users/kiva/ss/output" + i);
//        }

//        BootImage img = BootImage.fromFile("/Users/kiva/ss/mtk1.img");
//        img.unpack("/Users/kiva/ss/output");
//
        BootImage repackImg = BootImage.fromDir("/Users/kiva/ss/output");
        repackImg.repack("/Users/kiva/ss/output/jboot.img");

//        BootImage reunpackImg = BootImage.fromFile("/Users/kiva/ss/output/jboot.img");
//        reunpackImg.unpack("/Users/kiva/ss/output-reunpack");
    }
}
