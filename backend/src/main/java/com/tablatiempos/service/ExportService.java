package com.tablatiempos.service;

import com.tablatiempos.dto.GeneralRowView;
import com.tablatiempos.dto.RallyView;
import com.tablatiempos.dto.RecordEntryView;
import com.tablatiempos.dto.RecordView;
import com.tablatiempos.dto.StageCellView;
import com.tablatiempos.dto.StageView;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RallyViewService viewService;
    private final RecordService recordService;

    public ExportService(RallyViewService viewService, RecordService recordService) {
        this.viewService = viewService;
        this.recordService = recordService;
    }

    @Transactional(readOnly = true)
    public byte[] export(Long rallyId) throws IOException {
        RallyView view = viewService.view(rallyId);
        List<RecordView> records = recordService.all();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeRallySheet(workbook, styles, view);
            writeRecordsSheet(workbook, styles, records);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeRallySheet(Workbook workbook, Styles styles, RallyView view) {
        Sheet sheet = workbook.createSheet("Rally");
        List<StageView> stages = view.stages();
        int lastColumn = 2 + stages.size() + 4;

        Row title = sheet.createRow(0);
        title.setHeightInPoints(26);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(view.name());
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));

        Row subtitle = sheet.createRow(1);
        Cell subtitleCell = subtitle.createCell(0);
        String date = view.date() == null ? "" : " · " + view.date().format(DATE);
        subtitleCell.setCellValue("Tabla de tiempos creado por Bochi" + date);
        subtitleCell.setCellStyle(styles.subtitle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastColumn));

        Row header = sheet.createRow(3);
        header.setHeightInPoints(22);
        int column = 0;
        column = headerCell(header, column, "Pos.", styles);
        column = headerCell(header, column, "Piloto", styles);
        column = headerCell(header, column, "Coche", styles);
        for (StageView stage : stages) {
            String label = stage.name() == null || stage.name().isBlank()
                    ? stage.code()
                    : stage.code() + " · " + stage.name();
            column = headerCell(header, column, label, styles);
        }
        column = headerCell(header, column, "Total", styles);
        column = headerCell(header, column, "Dif. líder", styles);
        column = headerCell(header, column, "Dif. ant.", styles);
        headerCell(header, column, "Estado", styles);

        Map<Long, StageCellView> cells = new HashMap<>();
        for (StageView stage : stages) {
            for (StageCellView cell : stage.cells()) {
                cells.put(stage.id() * 100_000L + cell.entrantId(), cell);
            }
        }

        int rowIndex = 4;
        for (GeneralRowView general : view.general()) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(20);

            Cell position = row.createCell(0);
            if (general.position() != null) {
                position.setCellValue(general.position());
            } else {
                position.setCellValue("");
            }
            position.setCellStyle(styles.center);

            Cell pilot = row.createCell(1);
            pilot.setCellValue(general.name());
            pilot.setCellStyle(general.leader() ? styles.bold : styles.text);

            Cell car = row.createCell(2);
            car.setCellValue(general.car() == null ? "" : general.car());
            car.setCellStyle(styles.text);

            int cellColumn = 3;
            for (StageView stage : stages) {
                Cell stageCell = row.createCell(cellColumn++);
                StageCellView value = cells.get(stage.id() * 100_000L + general.entrantId());
                writeStageCell(stageCell, value, styles);
            }

            Cell total = row.createCell(cellColumn++);
            writeTime(total, general.totalMs(), styles.total);

            Cell difLeader = row.createCell(cellColumn++);
            writeGap(difLeader, general.difLeaderMs(), styles, general.leader());

            Cell difPrev = row.createCell(cellColumn++);
            writeGap(difPrev, general.difPrevMs(), styles, false);

            Cell status = row.createCell(cellColumn);
            status.setCellValue(statusLabel(general.status()));
            status.setCellStyle(statusStyle(general.status(), styles));
        }

        sheet.createFreezePane(3, 4);
        sheet.setColumnWidth(0, 7 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        for (int i = 0; i < stages.size(); i++) {
            sheet.setColumnWidth(3 + i, 20 * 256);
        }
        int base = 3 + stages.size();
        for (int i = 0; i < 4; i++) {
            sheet.setColumnWidth(base + i, 15 * 256);
        }
    }

    private void writeStageCell(Cell cell, StageCellView value, Styles styles) {
        if (value == null || value.millis() == null) {
            String label = "";
            if (value != null) {
                label = switch (value.kind()) {
                    case "EC" -> "EC";
                    case "RETIRED" -> "RET.";
                    default -> "";
                };
            }
            cell.setCellValue(label);
            cell.setCellStyle(styles.center);
            return;
        }
        boolean isPenalty = "PENALTY".equals(value.kind());
        if (value.record()) {
            cell.setCellValue("👑 " + formatTime(value.millis()));
            cell.setCellStyle(styles.record);
        } else if (isPenalty) {
            cell.setCellValue(formatTime(value.millis()));
            cell.setCellStyle(styles.penalty);
        } else if (value.best()) {
            cell.setCellValue(formatTime(value.millis()));
            cell.setCellStyle(styles.best);
        } else {
            cell.setCellValue(formatTime(value.millis()));
            cell.setCellStyle(styles.time);
        }
    }

    private void writeTime(Cell cell, Long millis, CellStyle style) {
        if (millis == null) {
            cell.setCellValue("");
        } else {
            cell.setCellValue(formatTime(millis));
        }
        cell.setCellStyle(style);
    }

    private void writeGap(Cell cell, Long millis, Styles styles, boolean leader) {
        if (millis == null || millis == 0) {
            cell.setCellValue(leader ? "—" : "");
            cell.setCellStyle(styles.center);
            return;
        }
        cell.setCellValue("+" + formatTime(millis));
        cell.setCellStyle(styles.gap);
    }

    private void writeRecordsSheet(Workbook workbook, Styles styles, List<RecordView> records) {
        Sheet sheet = workbook.createSheet("Récords");
        Row title = sheet.createRow(0);
        title.setHeightInPoints(24);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Récords históricos");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        Row header = sheet.createRow(2);
        header.setHeightInPoints(20);
        int column = 0;
        column = headerCell(header, column, "Tramo", styles);
        column = headerCell(header, column, "Tipo", styles);
        column = headerCell(header, column, "Pos.", styles);
        column = headerCell(header, column, "Piloto", styles);
        column = headerCell(header, column, "Coche", styles);
        column = headerCell(header, column, "Tiempo", styles);
        column = headerCell(header, column, "Rally", styles);
        headerCell(header, column, "Fecha", styles);

        int rowIndex = 3;
        for (RecordView record : records) {
            boolean first = true;
            for (RecordEntryView entry : record.top()) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(18);

                Cell name = row.createCell(0);
                name.setCellValue(first ? record.name() : "");
                name.setCellStyle(first ? styles.bold : styles.text);

                Cell type = row.createCell(1);
                type.setCellValue(first && "SHAKEDOWN".equals(record.type()) ? "SD" : "");
                type.setCellStyle(styles.center);

                Cell position = row.createCell(2);
                position.setCellValue(entry.position());
                position.setCellStyle(styles.center);

                Cell pilot = row.createCell(3);
                pilot.setCellValue(entry.pilot());
                pilot.setCellStyle(entry.position() == 1 ? styles.bold : styles.text);

                Cell car = row.createCell(4);
                car.setCellValue(entry.car() == null ? "" : entry.car());
                car.setCellStyle(styles.text);

                Cell time = row.createCell(5);
                time.setCellValue(formatTime(entry.millis()));
                time.setCellStyle(entry.position() == 1 ? styles.record : styles.time);

                Cell rally = row.createCell(6);
                rally.setCellValue(entry.rally() == null ? "" : entry.rally());
                rally.setCellStyle(styles.text);

                Cell date = row.createCell(7);
                date.setCellValue(entry.date() == null ? "" : entry.date().format(DATE));
                date.setCellStyle(styles.center);

                first = false;
            }
        }

        sheet.createFreezePane(0, 3);
        sheet.setColumnWidth(0, 26 * 256);
        sheet.setColumnWidth(1, 8 * 256);
        sheet.setColumnWidth(2, 7 * 256);
        sheet.setColumnWidth(3, 22 * 256);
        sheet.setColumnWidth(4, 22 * 256);
        sheet.setColumnWidth(5, 14 * 256);
        sheet.setColumnWidth(6, 28 * 256);
        sheet.setColumnWidth(7, 14 * 256);
    }

    private int headerCell(Row row, int column, String text, Styles styles) {
        Cell cell = row.createCell(column);
        cell.setCellValue(text);
        cell.setCellStyle(styles.header);
        return column + 1;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "RANKED" -> "Clasificado";
            case "EC" -> "EC";
            case "RETIRED" -> "Retirado";
            default -> status;
        };
    }

    private CellStyle statusStyle(String status, Styles styles) {
        return switch (status) {
            case "RANKED" -> styles.center;
            case "RETIRED" -> styles.retired;
            default -> styles.ec;
        };
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long ms = millis % 1000;
        return String.format("%d:%02d.%03d", minutes, seconds, ms);
    }

    private static final class Styles {
        final CellStyle title;
        final CellStyle subtitle;
        final CellStyle header;
        final CellStyle text;
        final CellStyle bold;
        final CellStyle center;
        final CellStyle total;
        final CellStyle time;
        final CellStyle gap;
        final CellStyle best;
        final CellStyle penalty;
        final CellStyle record;
        final CellStyle retired;
        final CellStyle ec;

        Styles(XSSFWorkbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            title = base(workbook);
            title.setFont(titleFont);

            Font subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitle = base(workbook);
            subtitle.setFont(subtitleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header = base(workbook);
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);

            text = base(workbook);
            bold = base(workbook);
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            bold.setFont(boldFont);

            center = base(workbook);
            center.setAlignment(HorizontalAlignment.CENTER);

            total = base(workbook);
            total.setAlignment(HorizontalAlignment.RIGHT);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            total.setFont(totalFont);

            time = base(workbook);
            time.setAlignment(HorizontalAlignment.RIGHT);

            gap = base(workbook);
            gap.setAlignment(HorizontalAlignment.RIGHT);
            Font gapFont = workbook.createFont();
            gapFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            gap.setFont(gapFont);

            best = base(workbook);
            best.setAlignment(HorizontalAlignment.RIGHT);
            best.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            best.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font bestFont = workbook.createFont();
            bestFont.setBold(true);
            best.setFont(bestFont);

            penalty = base(workbook);
            penalty.setAlignment(HorizontalAlignment.RIGHT);
            penalty.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            penalty.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font penaltyFont = workbook.createFont();
            penaltyFont.setColor(IndexedColors.DARK_RED.getIndex());
            penalty.setFont(penaltyFont);

            record = base(workbook);
            record.setAlignment(HorizontalAlignment.RIGHT);
            record.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
            record.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font recordFont = workbook.createFont();
            recordFont.setBold(true);
            recordFont.setColor(IndexedColors.PLUM.getIndex());
            record.setFont(recordFont);

            retired = base(workbook);
            retired.setAlignment(HorizontalAlignment.CENTER);
            Font retiredFont = workbook.createFont();
            retiredFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            retired.setFont(retiredFont);

            ec = base(workbook);
            ec.setAlignment(HorizontalAlignment.CENTER);
            Font ecFont = workbook.createFont();
            ecFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            ec.setFont(ecFont);
        }

        private CellStyle base(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }
    }
}
