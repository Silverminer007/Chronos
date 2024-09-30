package de.kjgstbarbara;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import de.kjgstbarbara.data.Game;
import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.data.Participant;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;

public class GameEvaluationUtils {

    public static void xlsxToPdf(InputStream xlsx, OutputStream outputStream) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(xlsx);

        PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream/*, new WriterProperties().addXmpMetadata()*/));
        Document document = new Document(pdf, PageSize.A4.rotate());

        XSSFSheet worksheet = workbook.getSheetAt(0);

        PdfDocumentInfo info = pdf.getDocumentInfo();
        info.setTitle(worksheet.getSheetName());

        PdfFont font = PdfFontFactory.createFont();

        int columns = worksheet.getRow(0).getLastCellNum();
        Table table = new Table(columns);
        table.setWidth(UnitValue.createPercentValue(100));
        for (Row row : worksheet) {
            for (int i = 0; i < columns; i++) {
                Cell cell = row.getCell(i);
                String cellValue = cell == null ? "" : switch (cell.getCellType()) {
                    case STRING -> cell.getStringCellValue();
                    case NUMERIC -> String.valueOf(BigDecimal.valueOf(cell.getNumericCellValue()));
                    default -> "";
                };
                com.itextpdf.layout.element.Cell cellPdf = new com.itextpdf.layout.element.Cell();
                cellPdf.add(new Paragraph(cellValue).setFont(font));
                table.addCell(cellPdf);
            }
        }

        document.add(table);
        document.close();
        workbook.close();
    }

    public static void createScoreboard(GameEvaluation gameEvaluation, OutputStream result) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(gameEvaluation.getName());
            java.util.List<Participant> scoreBoard = gameEvaluation.getScoreBoard();
            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Cell nameHeaderCell = header.createCell(0);
            nameHeaderCell.setCellValue("Name");
            nameHeaderCell.setCellStyle(headerStyle);
            for (int j = 0; j < gameEvaluation.getGames().size(); j++) {
                Cell pointsCell = header.createCell(j + 1);
                pointsCell.setCellValue(gameEvaluation.getGames().get(j).getName());
                pointsCell.setCellStyle(headerStyle);
            }
            Cell sumHeaderCell = header.createCell(gameEvaluation.getGames().size() + 1);
            sumHeaderCell.setCellValue("Gesamtpunktzahl");
            sumHeaderCell.setCellStyle(headerStyle);
            Cell maxPointsHeaderCell = header.createCell(gameEvaluation.getGames().size() + 2);
            maxPointsHeaderCell.setCellValue("Maximalpunktzahl");
            maxPointsHeaderCell.setCellStyle(headerStyle);
            Cell scoreHeaderCell = header.createCell(gameEvaluation.getGames().size() + 3);
            scoreHeaderCell.setCellValue("Score");
            scoreHeaderCell.setCellStyle(headerStyle);
            for (int i = 0; i < gameEvaluation.getParticipants().size(); i++) {
                Row row = sheet.createRow(i + 1);
                Cell nameCell = row.createCell(0);
                nameCell.setCellValue(scoreBoard.get(i).getName());

                for (int j = 0; j < gameEvaluation.getGames().size(); j++) {
                    Cell pointsCell = row.createCell(j + 1);
                    if (gameEvaluation.getGames().get(j).didParticipate(scoreBoard.get(i))) {
                        pointsCell.setCellValue(gameEvaluation.getGames().get(j).getPointsOf(scoreBoard.get(i)));
                    } else {
                        pointsCell.setCellValue("");
                    }
                }

                Cell sumCell = row.createCell(gameEvaluation.getGames().size() + 1);
                sumCell.setCellFormula("SUM(B" + (i + 2) + ":" + row.getCell(gameEvaluation.getGames().size()).getAddress() + ")");
                CellStyle sumCellStyle = workbook.createCellStyle();
                sumCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                XSSFFont font = workbook.createFont();
                font.setBold(true);
                sumCellStyle.setFont(font);
                sumCell.setCellStyle(sumCellStyle);

                Cell maxPointsCell = row.createCell(gameEvaluation.getGames().size() + 2);
                maxPointsCell.setCellValue(gameEvaluation.getMaxPointsFor(scoreBoard.get(i)));
                maxPointsCell.setCellStyle(sumCellStyle);

                Cell scoreCell = row.createCell(gameEvaluation.getGames().size() + 3);
                scoreCell.setCellFormula("ROUND(" + sumCell.getAddress() + "/" + maxPointsCell.getAddress() + "*100,2)&\"%\"");
                scoreCell.setCellStyle(sumCellStyle);
            }
            workbook.write(result);
        }
    }

    public static void createGroupOverview(Game game, OutputStream result) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(game.getName());
            Row header = sheet.createRow(0);
            CellStyle headerStyleFirst = workbook.createCellStyle();
            headerStyleFirst.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            XSSFFont headerFontFirst = workbook.createFont();
            headerFontFirst.setBold(true);
            headerStyleFirst.setFont(headerFontFirst);
            CellStyle headerStyleSecond = workbook.createCellStyle();
            headerStyleSecond.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            XSSFFont headerFontSecond = workbook.createFont();
            headerFontSecond.setFontHeight(20);
            headerFontSecond.setBold(true);
            headerStyleSecond.setFont(headerFontFirst);
            for (int i = 0; i < game.getGameGroups().size(); i++) {
                Cell groupNameCell = header.createCell(i);
                groupNameCell.setCellValue(game.getGameGroups().get(i).getName());
                groupNameCell.setCellStyle(i % 2 == 0 ? headerStyleFirst : headerStyleSecond);
            }
            CellStyle cellStyleFirst = workbook.createCellStyle();
            cellStyleFirst.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle cellStyleSecond = workbook.createCellStyle();
            cellStyleSecond.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            boolean hasNext = true;
            int rowID = 1;
            while (hasNext) {
                hasNext = false;
                Row row = sheet.createRow(rowID);
                for (int i = 0; i < game.getGameGroups().size(); i++) {
                    if (game.getGameGroups().get(i).getParticipants().size() >= rowID) {
                        hasNext = true;
                        Cell nameCell = row.createCell(i);
                        nameCell.setCellValue(game.getGameGroups().get(i).getParticipants().get(rowID - 1).getName());
                        nameCell.setCellStyle(i % 2 == 0 ? cellStyleFirst : cellStyleSecond);
                    }
                }
                rowID++;
            }
            workbook.write(result);
        }
    }
}