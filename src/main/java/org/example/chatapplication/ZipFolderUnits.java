//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//import java.util.zip.ZipOutputStream;
//
//public class ZipFolderUnits {
//
//    public static void zipFolder(File folder, File zipFile) throws IOException {
//        try (FileOutputStream fos = new FileOutputStream(zipFile);
//             ZipOutputStream zos = new ZipOutputStream(fos)) {
//            zipFolderHelper(folder, folder.getName(), zos);
//        }
//    }
//
//    private static void zipFolderHelper(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
//        for (File file : folder.listFiles()) {
//            if (file.isDirectory()) {
//                zipFolderHelper(file, parentFolder + "/" + file.getName(), zos);
//            } else {
//                try (FileInputStream fis = new FileInputStream(file)) {
//                    String zipEntryName = parentFolder + "/" + file.getName();
//                    zos.putNextEntry(new ZipEntry(zipEntryName));
//
//                    byte[] buffer = new byte[4096];
//                    int bytesRead;
//                    while ((bytesRead = fis.read(buffer)) != -1) {
//                        zos.write(buffer, 0, bytesRead);
//                    }
//                    zos.closeEntry();
//                }
//            }
//        }
//    }
//
//    public static void unzipFolder(File zipFile, File destDir) throws IOException {
//        try (FileInputStream fis = new FileInputStream(zipFile);
//             ZipInputStream zis = new ZipInputStream(fis)) {
//            ZipEntry entry;
//            while ((entry = zis.getNextEntry()) != null) {
//                File newFile = new File(destDir, entry.getName());
//                if (entry.isDirectory()) {
//                    newFile.mkdirs();
//                }
//
