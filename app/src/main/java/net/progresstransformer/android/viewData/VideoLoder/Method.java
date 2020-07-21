package net.progresstransformer.android.viewData.VideoLoder;


import net.progresstransformer.android.viewData.Database.DBHelper;
import net.progresstransformer.android.viewData.MainActivity;

import java.io.File;

public class Method {
    private static DBHelper dbHelper;
    private static MainActivity mainActivity = new MainActivity();

    public static void load_Directory_Files(File directory) {
        File[] fileList = directory.listFiles();
        if (fileList != null && fileList.length > 0) {
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {
                    load_Directory_Files(fileList[i]);
                } else {
                    String name = fileList[i].getName().toLowerCase();
                    for (String extension : Constant.videoExtensions) {
                        //check the type of file
                        if (name.endsWith(extension) && !Constant.allMediaList.contains(fileList[i])) {
                            Constant.allMediaList.add(fileList[i]);


                            //when we found file
                            break;
                        }
                    }
                    for (String extension : Constant.audioExtensions) {
                        //check the type of file
                        if (name.endsWith(extension) && !Constant.allaudioList.contains(fileList[i])) {
                            Constant.allaudioList.add(fileList[i]);


                            //when we found file
                            break;
                        }
                    }
                    if (name.endsWith(Constant.pdfExtensions) && !Constant.allpdfList.contains(fileList[i])) {
                        Constant.allpdfList.add(fileList[i]);
                    }
                }
            }
        }
    }

}
