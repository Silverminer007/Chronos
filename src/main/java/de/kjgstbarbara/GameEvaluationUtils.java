package de.kjgstbarbara;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import de.kjgstbarbara.data.Game;
import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.data.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;

public class GameEvaluationUtils {
    private static final Logger LOGGER = LogManager.getLogger(GameEvaluationUtils.class);

    public static void xlsxToPdf(InputStream xlsx, OutputStream pdf) throws IOException, DocumentException {
        XSSFWorkbook workbook = new XSSFWorkbook(xlsx);

        Document document = new Document();
        PdfWriter.getInstance(document, pdf);
        document.open();

        XSSFSheet worksheet = workbook.getSheetAt(0);
        document.addTitle(worksheet.getSheetName());
        PdfPTable table = new PdfPTable(worksheet.getRow(0).getLastCellNum());
        for (Row row : worksheet) {
            for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                Cell cell = row.getCell(i);
                String cellValue = switch (cell.getCellType()) {
                    case STRING -> cell.getStringCellValue();
                    case NUMERIC -> String.valueOf(BigDecimal.valueOf(cell.getNumericCellValue()));
                    default -> "";
                };
                PdfPCell cellPdf = new PdfPCell(new Phrase(cellValue, getCellStyle(cell)));
                setBackgroundColor(cell, cellPdf);
                setCellAlignment(cell, cellPdf);
                table.addCell(cellPdf);
            }
        }

        document.add(table);
        document.close();
        workbook.close();
    }

    private static void setCellAlignment(Cell cell, PdfPCell cellPdf) {
        CellStyle cellStyle = cell.getCellStyle();

        HorizontalAlignment horizontalAlignment = cellStyle.getAlignment();

        switch (horizontalAlignment) {
            case LEFT:
                cellPdf.setHorizontalAlignment(Element.ALIGN_LEFT);
                break;
            case CENTER:
                cellPdf.setHorizontalAlignment(Element.ALIGN_CENTER);
                break;
            case JUSTIFY:
            case FILL:
                cellPdf.setVerticalAlignment(Element.ALIGN_JUSTIFIED);
                break;
            case RIGHT:
                cellPdf.setHorizontalAlignment(Element.ALIGN_RIGHT);
                break;
        }
    }

    private static void setBackgroundColor(Cell cell, PdfPCell cellPdf) {
        short bgColorIndex = cell.getCellStyle()
                .getFillForegroundColor();
        if (bgColorIndex != IndexedColors.AUTOMATIC.getIndex()) {
            XSSFColor bgColor = (XSSFColor) cell.getCellStyle()
                    .getFillForegroundColorColor();
            if (bgColor != null) {
                byte[] rgb = bgColor.getRGB();
                if (rgb != null && rgb.length == 3) {
                    cellPdf.setBackgroundColor(new BaseColor(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF));
                }
            }
        }
    }

    private static Font getCellStyle(Cell cell) throws DocumentException, IOException {
        Font font = new Font();
        CellStyle cellStyle = cell.getCellStyle();
        org.apache.poi.ss.usermodel.Font cellFont = cell.getSheet()
                .getWorkbook()
                .getFontAt(cellStyle.getFontIndex());

        if (cellFont.getItalic()) {
            font.setStyle(Font.ITALIC);
        }

        if (cellFont.getStrikeout()) {
            font.setStyle(Font.STRIKETHRU);
        }

        if (cellFont.getUnderline() == 1) {
            font.setStyle(Font.UNDERLINE);
        }

        short fontSize = cellFont.getFontHeightInPoints();
        font.setSize(fontSize);

        if (cellFont.getBold()) {
            font.setStyle(Font.BOLD);
        }

        String fontName = cellFont.getFontName();
        if (FontFactory.isRegistered(fontName)) {
            font.setFamily(fontName);
        } else {
            LOGGER.warn("Unsupported font type: {}", fontName);
            font.setFamily("Helvetica");
        }

        return font;
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