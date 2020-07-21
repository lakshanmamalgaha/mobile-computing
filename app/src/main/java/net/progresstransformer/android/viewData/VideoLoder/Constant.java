package net.progresstransformer.android.viewData.VideoLoder;

import java.io.File;
import java.util.ArrayList;

public class Constant {
    public static String[] videoExtensions = {".mp4", ".ts", ".mkv", ".mov",
            ".3gp", ".mv2", ".m4v", ".webm", ".mpeg1", ".mpeg2", ".mts", ".ogm",
            ".bup", ".dv", ".flv", ".m1v", ".m2ts", ".mpeg4", ".vlc", ".3g2",
            ".avi", ".mpeg", ".mpg", ".wmv", ".asf"};
    public static String pdfExtensions = ".pdf";

    public static String[] audioExtensions = {".mp3", ".wav"};

    //all loaded files will be here
    public static ArrayList<File> allMediaList = new ArrayList<>();

    public static ArrayList<File> allpdfList = new ArrayList<>();

    public static ArrayList<File> allaudioList = new ArrayList<>();

}
