package com.floop.phonebook.scratch;


import com.floop.phonebook.dto.PhonebookEntryRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;

public class PoiPlayground {



    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String[] words = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    private static String readString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
           return null;
        }

       return cell.getStringCellValue();

    }
    private static LocalDate readLocalDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        return cell.getLocalDateTimeCellValue().toLocalDate();
    }

    public static void main (String[] args) throws Exception {

        try(Workbook workbook = new XSSFWorkbook();
            FileOutputStream fileOutputStream = new FileOutputStream("test_export.xlsx");
        ) {
            Sheet sheet = workbook.createSheet("Test");
            Row row1 = sheet.createRow(0);
            Row row2 = sheet.createRow(1);
            Cell cell1 = row1.createCell(0);
            Cell cell2 = row1.createCell(1);
            Cell cell3 = row2.createCell(0);
            Cell cell4 = row2.createCell(1);
            cell1.setCellValue("Ad");
            cell2.setCellValue("Soyad");
            cell3.setCellValue("Huseyn");
            cell4.setCellValue("Asadzada");
            workbook.write(fileOutputStream);

        }catch(Exception e){
            System.out.println("ERROR --> " + e.getMessage());
        }











    }

//    public static void main(String[] args) throws Exception {
//
//        System.out.println(LocalDate.parse("2026-01-01"));
//        System.out.println(Integer.parseInt("555"));
//        System.out.println(Boolean.parseBoolean("abc"));
//        LocalDate.parse("abc");
//
//        FileInputStream fis = new FileInputStream("phonebook-test-import.xlsx");
//        Workbook workbook = WorkbookFactory.create(fis);
//        Sheet sheet = workbook.getSheetAt(0);
//
//
//        for (Row row : sheet) {
//            for (Cell cell : row) {
//                if (row.getRowNum() == 0){
//                    System.out.println( cell.getColumnIndex()+ ": " + cell);
//                }
//
//            }
//
//            if (row.getRowNum() >= 1) {
//                PhonebookEntryRequest request = new PhonebookEntryRequest();
//                request.setName(capitalize(readString(row, 0)));
//                request.setSurname(capitalize(readString(row, 1)));
//                request.setNationalId(capitalize(readString(row, 2)));
//                request.setFin(capitalize(readString(row, 4)));
//                request.setAddress(capitalize(readString(row, 5)));
//                request.setCity(capitalize(readString(row, 6)));
//                request.setNumber(capitalize(readString(row, 7)));
//                request.setActivatedDate(readLocalDate(row, 8));
//                request.setDateOfBirth(readLocalDate(row, 3));
//                request.setStopDate(readLocalDate(row, 9));
//
//                System.out.println(request.getName() + " " + request.getSurname() + " " +
//                        request.getNumber() + " " + request.getActivatedDate() + " " +
//                        request.getStopDate());
//
//            }
//        }
//
//
//    }


}